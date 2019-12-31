package cn.wanghaomiao;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

/** 抓取携程 */
public class TestCTRIP {

  public static void main(String[] args) throws Exception {
    for (int i = 2; i < 3; i++) {
      Map map = new HashMap();
      map.put("cityid", "105");
      map.put("districtId", "822");
      map.put("pindex", i + "");
      map.put("pSize", "15");
      String result =
          sendHttps(
              "https://m.ctrip.com/restapi/soa2/12455/prod/json/searchProduct?_fxpcqlniredt=09031133110201642129",
              map,
              "utf-8");
      Map resultMap = JSONObject.parseObject(result, Map.class);
      System.out.println(resultMap);
      List<Map> list = (List<Map>) resultMap.get("product");
      Date time = new Date();
      for (Map map1 : list) {
        Hotel hotel = new Hotel();
        // 加了非空判断，因为爬过来的数据字段可能为空
        hotel.setPid(map1.get("pid").toString());
        if (map1.get("pname") != null) {
          hotel.setPname(map1.get("pname").toString());
        }
        if (map1.get("zone") != null) {
          hotel.setZone(map1.get("zone").toString());
        }
        if (map1.get("rname") != null) {
          hotel.setRname(map1.get("rname").toString());
        }
        if (map1.get("zoneId") != null) {
          hotel.setZoneId(map1.get("zoneId").toString());
        }
        if (map1.get("pos") != null) {
          // 这是坐标，本身是对象，所以做了二次分解
          Map mapPos = (Map) map1.get("pos");
          if (mapPos.get("lng") != null) {
            hotel.setLng(mapPos.get("lng").toString());
          }
          if (mapPos.get("lat") != null) {
            hotel.setLat(mapPos.get("lat").toString());
          }
        }

        hotel.setCreateTime(time);
        System.out.println(hotel);
      }
      Thread.sleep(3000);
    }
  }

  /** 绕过验证 */
  public static SSLContext createIgnoreVerifySSL()
      throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sc = SSLContext.getInstance("SSLv3");

    // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
    X509TrustManager trustManager =
        new X509TrustManager() {
          @Override
          public void checkClientTrusted(
              java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString)
              throws CertificateException {}

          @Override
          public void checkServerTrusted(
              java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString)
              throws CertificateException {}

          @Override
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
          }
        };

    sc.init(null, new TrustManager[] {trustManager}, null);
    return sc;
  }
  /**
   * 模拟请求https
   *
   * @param url 资源地址
   * @param map 参数列表
   * @param encoding 编码
   * @return
   * @throws NoSuchAlgorithmException
   * @throws KeyManagementException
   * @throws IOException
   * @throws ClientProtocolException
   */
  public static String sendHttps(String url, Map<String, String> map, String encoding)
      throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException,
          IOException {
    String body = "";
    // 采用绕过验证的方式处理https请求
    SSLContext sslcontext = createIgnoreVerifySSL();

    // 设置协议http和https对应的处理socket链接工厂的对象
    Registry<ConnectionSocketFactory> socketFactoryRegistry =
        RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.INSTANCE)
            .register("https", new SSLConnectionSocketFactory(sslcontext))
            .build();
    PoolingHttpClientConnectionManager connManager =
        new PoolingHttpClientConnectionManager(socketFactoryRegistry);
    HttpClients.custom().setConnectionManager(connManager);

    // 创建自定义的httpclient对象
    CloseableHttpClient client = HttpClients.custom().setConnectionManager(connManager).build();
    //        CloseableHttpClient client = HttpClients.createDefault();

    // 创建post方式请求对象
    HttpPost httpPost = new HttpPost(url);

    // 装填参数
    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    if (map != null) {
      for (Map.Entry<String, String> entry : map.entrySet()) {
        nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
      }
    }
    // 设置参数到请求对象中
    httpPost.setEntity(new UrlEncodedFormEntity(nvps, encoding));

    System.out.println("请求地址：" + url);
    System.out.println("请求参数：" + nvps.toString());

    // 设置header信息
    // 指定报文头【Content-type】、【User-Agent】
    httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
    httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

    // 执行请求操作，并拿到结果（同步阻塞）
    CloseableHttpResponse response = client.execute(httpPost);
    // 获取结果实体
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      // 按指定编码转换结果实体为String类型
      body = EntityUtils.toString(entity, encoding);
    }
    EntityUtils.consume(entity);
    // 释放链接
    response.close();
    return body;
  }

  /**
   * 模拟请求http
   *
   * @param url 资源地址
   * @param map 参数列表
   * @param encoding 编码
   * @return
   * @throws ParseException
   * @throws IOException
   */
  public static String sendHttp(String url, Map<String, String> map, String encoding)
      throws ParseException, IOException {
    String body = "";

    // 创建httpclient对象
    CloseableHttpClient client = HttpClients.createDefault();
    // 创建post方式请求对象
    HttpPost httpPost = new HttpPost(url);

    // 装填参数
    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    if (map != null) {
      for (Map.Entry<String, String> entry : map.entrySet()) {
        nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
      }
    }
    // 设置参数到请求对象中
    httpPost.setEntity(new UrlEncodedFormEntity(nvps, encoding));

    System.out.println("请求地址：" + url);
    System.out.println("请求参数：" + nvps.toString());

    // 设置header信息
    // 指定报文头【Content-type】、【User-Agent】
    httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
    httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

    // 执行请求操作，并拿到结果（同步阻塞）
    CloseableHttpResponse response = client.execute(httpPost);
    // 获取结果实体
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      // 按指定编码转换结果实体为String类型
      body = EntityUtils.toString(entity, encoding);
    }
    EntityUtils.consume(entity);
    // 释放链接
    response.close();
    return body;
  }

  static class Hotel {
    String id;
    String pid;
    String pname;
    String zone;
    String lng;
    String lat;
    String zoneId;
    String rname;
    Date createTime;

    @Override
    public String toString() {
      return "Hotel{" +
              "id='" + id + '\'' +
              ", pid='" + pid + '\'' +
              ", pname='" + pname + '\'' +
              ", zone='" + zone + '\'' +
              ", lng='" + lng + '\'' +
              ", lat='" + lat + '\'' +
              ", zoneId='" + zoneId + '\'' +
              ", rname='" + rname + '\'' +
              ", createTime=" + createTime +
              '}';
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getPid() {
      return pid;
    }

    public void setPid(String pid) {
      this.pid = pid;
    }

    public String getPname() {
      return pname;
    }

    public void setPname(String pname) {
      this.pname = pname;
    }

    public String getZone() {
      return zone;
    }

    public void setZone(String zone) {
      this.zone = zone;
    }

    public String getLng() {
      return lng;
    }

    public void setLng(String lng) {
      this.lng = lng;
    }

    public String getLat() {
      return lat;
    }

    public void setLat(String lat) {
      this.lat = lat;
    }

    public String getZoneId() {
      return zoneId;
    }

    public void setZoneId(String zoneId) {
      this.zoneId = zoneId;
    }

    public String getRname() {
      return rname;
    }

    public void setRname(String rname) {
      this.rname = rname;
    }

    public Date getCreateTime() {
      return createTime;
    }

    public void setCreateTime(Date createTime) {
      this.createTime = createTime;
    }
  }
}
