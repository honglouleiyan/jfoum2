package net.jforum.util.aqara;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Util {

    /**
     * md5 - 32位
     * @param sourceStr
     * @return
     * @throws Exception
     */
    public static String MD5_32(String sourceStr)throws Exception {
        String result = "";
        try {
            byte b[] = md5(sourceStr.getBytes());
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e);
        }
        return result;
    }

    public static void main(String[] args)
    {
        try
        {
           String s =  MD5_32("abdcedsdsjdskhk34DDD") ;
            System.out.println(s);

        }catch (Exception e)
        {

        }
    }


    /**
     * 16位
     * @param sourceStr
     * @return
     * @throws Exception
     */
    public static String MD5_16(String sourceStr)throws Exception {
        String res = MD5_32(sourceStr);
        String result = res.substring(8, 24);
        System.out.println("MD5(" + sourceStr + ",16) = " + result);
        return result;
    }

    /**
     * 获取byte[]的md5值
     * @param bytes byte[]
     * @return md5
     * @throws Exception
     */
    public static byte[] md5(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(bytes);
        return md.digest();
    }

}
