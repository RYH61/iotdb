package org.apache.iotdb.db.utils.license;

import org.apache.iotdb.commons.conf.IoTDBConstant;
import org.apache.iotdb.db.conf.IoTDBConfig;
import org.apache.iotdb.db.conf.IoTDBDescriptor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;

/** the same as CEA */
public class ParseLicenseUtilV3 {

  private static final Logger logger = LoggerFactory.getLogger(ParseLicenseUtilV3.class);
  private static final IoTDBConfig config = IoTDBDescriptor.getInstance().getConfig();
  private Key publicKey;

  private int keySize;

  public void initParams(String crtPath) throws Exception {
    publicKey = getPublicKeyFromCrt(crtPath);
    if (publicKey == null) {
      throw new Exception("parse the public key failed");
    }
    keySize = getKeySize(publicKey, RSAPublicKeySpec.class);
  }

  /**
   * 获取公钥
   *
   * @param crtPath 证书路径
   */
  private PublicKey getPublicKeyFromCrt(String crtPath)
      throws CertificateException, FileNotFoundException {
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    File file = new File(crtPath);
    try (FileInputStream in = new FileInputStream(file)) {
      Certificate crt = cf.generateCertificate(in);
      return crt.getPublicKey();
    } catch (IOException e) {
      logger.error("get public key from crt failed", e);
    }
    return null;
  }

  public String readLicenseJsonString(String licensePath) throws Exception {
    byte[] licenseData = readLicenseData(licensePath);
    return new String(decryptRSA(publicKey, Base64.getDecoder().decode(licenseData)));
  }

  private byte[] readLicenseData(String licensePath) throws IOException {
    InputStream is = new FileInputStream(licensePath);
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    byte[] temp = new byte[1024];
    int count;
    while (-1 != (count = is.read(temp))) {
      bao.write(temp, 0, count);
    }
    temp = bao.toByteArray();
    bao.close();
    is.close();
    return temp;
  }

  private byte[] decryptRSA(Key publicKey, byte[] data) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, publicKey);
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    int offset = 0;
    byte[] tmp;
    int blockSize = keySize / 8;
    while (data.length - offset > 0) {
      if (data.length - offset > blockSize) {
        tmp = cipher.doFinal(data, offset, blockSize);
      } else {
        tmp = cipher.doFinal(data, offset, data.length - offset);
      }
      offset += blockSize;
      bout.write(tmp, 0, tmp.length);
    }
    tmp = bout.toByteArray();
    bout.close();
    return tmp;
  }

  private static int getKeySize(Key key, Class<? extends KeySpec> keySpecImpl) throws Exception {
    String algorithm = key.getAlgorithm();
    KeyFactory keyFact = KeyFactory.getInstance(algorithm);
    KeySpec keySpec = keyFact.getKeySpec(key, keySpecImpl);
    Method getModulus = keySpecImpl.getDeclaredMethod("getModulus");
    return ((BigInteger) getModulus.invoke(keySpec)).toString(2).length();
  }

  public static boolean verifyTheLicense() {
    ParseLicenseUtilV3 pl = new ParseLicenseUtilV3();
    // 初始化公钥文件
    try {
      pl.initParams(getPublicKeyFile());
    } catch (Exception e) {
      logger.error("公钥文件丢失===>请联系管理员", e);
      return false;
    }

    String str = "";
    // 解析公钥文件
    try {
      str = pl.readLicenseJsonString(getLicenseFile());
    } catch (Exception e1) {
      logger.error("License文件丢失===>请联系管理员", e1);
      return false;
    }
    JSONObject jo = new JSONObject(str);

    // 判断是否为永久使用
    String endDate = "";
    String startDate = "";
    try {
      endDate = jo.getString("endDate");
      startDate = jo.getString("startDate");
    } catch (Exception e) {
      logger.info("startDate={}, endDate={}, 永久使用", startDate, endDate);
    }

    logger.info("startDate={}, endDate={}", startDate, endDate);

    JSONArray ja = (JSONArray) jo.get("productInfoList");
    StringBuilder macList = new StringBuilder();

    int xcloudNum = 0;
    int taskNum = 0;
    // 遍历productInfoList申请license的产品信息数组
    for (int i = 0; i < ja.length(); i++) {
      // 根据每个productInfoList中的元素，赋予对应元素的mac列表，xcloud、task节点数
      try {
        // 叠加所有的mac地址
        macList.append(((JSONObject) ja.get(i)).get("macList"));
      } catch (Exception e) {
        macList.append("");
        logger.error("parse the mac list error", e);
      }

      /**
       * try { // 叠加所有的节点数 xcloudNum = xcloudNum + Integer.parseInt( (new JSONObject(((JSONObject)
       * ja.get(i)).get("customParamJson").toString())) .get("xcloudNum") .toString()); } catch
       * (Exception e) { // 异常归零处理 xcloudNum = xcloudNum + 0; logger.error("parse the xcloud num
       * error", e); } try { // 叠加所有的节点数 taskNum = taskNum + Integer.parseInt( (new
       * JSONObject(((JSONObject) ja.get(i)).get("customParamJson").toString())) .get("taskNum")
       * .toString()); } catch (Exception e) { // 异常归零处理 taskNum = taskNum + 0; logger.error("parse
       * the task num error", e); }
       */
    }

    // check the mac address
    String macAddress = ComputerInfoUtils.getMacAddress();
    logger.info("allow mac address={}, current mac address={}", macList, macAddress);
    boolean checkMacAddress = false;
    if (macAddress != null) {
      String macAddressList = macList.toString().substring(2, macList.toString().length() - 2);
      String[] macAddressArray;
      if (macAddressList.contains(",")) {
        macAddressArray = macAddressList.split(",");
        for (int i = 0; i < macAddressArray.length; i++) {
          if (macAddress.contains(macAddressArray[i].trim().replaceAll("\"", ""))) {
            checkMacAddress = true;
            logger.info("client validates the certificate successfully!");
            break;
          }
        }
      } else {
        if (macAddress.contains(macAddressList.trim())) {
          checkMacAddress = true;
          logger.info("The client validates the certificate successfully!");
        } else {
          logger.error(
              "Wrong Mac address, service is starting, you will see an incomplete management interface!");
        }
      }
    }

    if (!checkMacAddress) {
      return false;
    }

    // check the license expired or not
    long time = System.currentTimeMillis();
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    if (startDate.equals("") || endDate.equals("")) {
      logger.info("the license is forever, startDate={}, endDate={}", startDate, endDate);
      return true;
    } else {
      try {
        if (dateFormat.parse(startDate).getTime() <= time
            && time <= dateFormat.parse(endDate).getTime()) {
          logger.info(
              "the license is valid, now time={}, startDate={}, endDate={}",
              time,
              startDate,
              endDate);
          return true;
        } else {
          logger.error(
              "the license have been expired, startDate={}, endDate={}", startDate, endDate);
          return false;
        }
      } catch (ParseException e) {
        logger.error("parse the license date failed", e);
      }
    }
    return true;
  }

  public static String getPublicKeyFile() {
    // 1. find in relative path
    String urlString = System.getProperty(IoTDBConstant.IOTDB_CONF, null);
    if (urlString == null) {
      urlString = System.getProperty(IoTDBConstant.IOTDB_HOME, null);
      if (urlString != null) {
        urlString =
            urlString
                + File.separatorChar
                + "conf"
                + File.separatorChar
                + config.getPublicKeyFileName();
      } else {
        logger.warn(
            "Cannot find IOTDB_HOME or IOTDB_CONF environment variable when loading "
                + "public key file {}",
            config.getPublicKeyFileName());
      }
    } else {
      urlString += (File.separatorChar + config.getPublicKeyFileName());
    }

    if (urlString != null) {
      File file = new File(urlString);
      if (file.exists()) {
        logger.info("found the public key path={}", urlString);
        return urlString;
      }
    }

    logger.info(
        "not found in default conf file, use the configure public key path={}",
        config.getPublicKeyFileName());
    return config.getPublicKeyFileName();
  }

  public static String getLicenseFile() {
    // 1. find in relative path
    String urlString = System.getProperty(IoTDBConstant.IOTDB_CONF, null);
    if (urlString == null) {
      urlString = System.getProperty(IoTDBConstant.IOTDB_HOME, null);
      if (urlString != null) {
        urlString =
            urlString
                + File.separatorChar
                + "conf"
                + File.separatorChar
                + config.getLicenseFileName();
      } else {
        logger.warn(
            "Cannot find IOTDB_HOME or IOTDB_CONF environment variable when loading "
                + "license file {}",
            config.getLicenseFileName());
      }
    } else {
      urlString += (File.separatorChar + config.getLicenseFileName());
    }

    if (urlString != null) {
      File file = new File(urlString);
      if (file.exists()) {
        logger.info("found the license path={}", urlString);
        return urlString;
      }
    }

    logger.info(
        "not found in default conf file, use the configure license path={}",
        config.getLicenseFileName());
    return config.getLicenseFileName();
  }
}
