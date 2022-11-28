package org.apache.iotdb.db.utils.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComputerInfoUtils {

  private static final Logger logger = LoggerFactory.getLogger(ComputerInfoUtils.class);
  private static String macAddressStr = null;
  private static String computerName = System.getenv().get("COMPUTERNAME");

  private static final String[] windowsCommand = {"ipconfig", "/all"};
  private static final String[] ifconfigLinuxCommand = {"/sbin/ifconfig", "-a"};
  private static final String[] ipLinuxCommand = {"/sbin/ip", "address"};
  private static final Pattern macPattern =
      Pattern.compile(".*((:?[0-9a-f]{2}[-:]){5}[0-9a-f]{2}).*", Pattern.CASE_INSENSITIVE);

  private static final List<String> invalidMacAddress =
      Arrays.asList(
          "00-00-00-00-00-E0", "ff:ff:ff:ff:ff:ff", "FF:FF:FF:FF:FF:FF", "00:00:00:00:00:00");

  /**
   * get the mac address
   *
   * @return
   * @throws IOException
   */
  private static final List<String> getMacAddressList() throws IOException {
    ArrayList<String> macAddressList = new ArrayList<String>();
    final String os = System.getProperty("os.name");
    String command[];
    boolean isLinux = false;
    if (os.startsWith("Windows")) {
      command = windowsCommand;
    } else if (os.startsWith("Linux")) {
      command = ipLinuxCommand;
      isLinux = true;
    } else {
      throw new IOException("Unknow operating system:" + os);
    }

    macAddressList = getMacAddressList(command);
    if (macAddressList.isEmpty() && isLinux) {
      logger.warn(
          "can not get mac address using command={}, try using={}", command, ifconfigLinuxCommand);
      command = ifconfigLinuxCommand;
      macAddressList = getMacAddressList(command);
    }
    if (macAddressList.isEmpty()) {
      logger.error("get mac address failed");
    }
    return macAddressList.stream().distinct().collect(Collectors.toList());
  }

  /**
   * get the mac address
   *
   * @return
   * @throws IOException
   */
  private static final ArrayList<String> getMacAddressList(String[] command) throws IOException {
    final ArrayList<String> macAddressList = new ArrayList<String>();
    // 执行命令
    final Process process = Runtime.getRuntime().exec(command);

    BufferedReader bufReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    for (String line = null; (line = bufReader.readLine()) != null; ) {
      String[] strArr = line.split(" ");
      for (String str : strArr) {
        Matcher matcher = macPattern.matcher(str);
        if (matcher.matches()) {
          macAddressList.add(matcher.group(1));
          // macAddressList.add(matcher.group(1).replaceAll("[-:]",
          // ""));//去掉MAC中的“-”
        }
      }
    }

    process.destroy();
    bufReader.close();
    return macAddressList;
  }

  /**
   * 获取一个网卡地址（多个网卡时从中获取一个）
   *
   * @return
   */
  public static String getMacAddress() {
    if (macAddressStr == null || macAddressStr.equals("")) {
      StringBuffer sb = new StringBuffer(); // 存放多个网卡地址用，目前只取一个非0000000000E0隧道的值
      try {
        List<String> macList = getMacAddressList();
        for (Iterator<String> iter = macList.iterator(); iter.hasNext(); ) {
          String amac = iter.next();
          boolean valid = true;
          for (String invalidAddress : invalidMacAddress) {
            if (amac.equals(invalidAddress)) {
              valid = false;
            }
          }
          if (valid) {
            sb.append(amac);
            sb.append(",");
          }
        }
      } catch (IOException e) {
        logger.error("get mac address failed", e);
      }
      macAddressStr = sb.toString();
    }
    return macAddressStr.toUpperCase();
  }

  /**
   * 获取电脑名
   *
   * @return
   */
  public static String getComputerName() {
    if (computerName == null || computerName.equals("")) {
      computerName = System.getenv().get("COMPUTERNAME");
    }
    return computerName;
  }

  /**
   * 获取客户端IP地址
   *
   * @return
   */
  public static String getIpAddrAndName() throws IOException {
    return InetAddress.getLocalHost().toString();
  }

  /**
   * 获取客户端IP地址
   *
   * @return
   */
  public static String getIpAddr() throws IOException {
    return InetAddress.getLocalHost().getHostAddress().toString();
  }

  /**
   * 获取电脑唯一标识
   *
   * @return
   */
  public static String getComputerID() {
    String id = getMacAddress();
    if (id == null || id.equals("")) {
      try {
        id = getIpAddrAndName();
      } catch (IOException e) {
        logger.error("get computer ID failed", e);
      }
    }
    return computerName;
  }

  /** 限制创建实例 */
  private ComputerInfoUtils() {}

  public static void main(String[] args) throws IOException {
    System.out.println(ComputerInfoUtils.getMacAddress());
    System.out.println(ComputerInfoUtils.getComputerName());
    System.out.println(ComputerInfoUtils.getIpAddr());
    System.out.println(ComputerInfoUtils.getIpAddrAndName());
  }
}
