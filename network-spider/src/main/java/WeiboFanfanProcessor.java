import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.SimpleProxyProvider;
import us.codecraft.webmagic.selector.JsonPathSelector;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.pipeline.FilePipeline;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;

public class WeiboFanfanProcessor implements PageProcessor {

    private Site site = Site.me().setRetryTimes(5).setSleepTime(2500);
    private boolean load;

    private String[] uids;
    private HashSet<String> uidmap;
    private HashSet<String> al_uidmap;

    private HashMap<String, Integer> uid2maxpg;
    private HashMap<String, Integer> uid2fanfan;

    private FileOutputStream fos;
    private OutputStreamWriter osw;
    private BufferedWriter bw;

    private Integer total_solve;
    private Integer user_num;

    WeiboFanfanProcessor() {
        load = false;
        uidmap = new HashSet<String>();
        al_uidmap = new HashSet<String>();
        uid2maxpg = new HashMap<String, Integer>();
        uid2fanfan = new HashMap<String, Integer>();

        total_solve = 0;
        user_num = 0;

        String res_file = "fanfan-2020-05-16.txt";
        try {
            fos = new FileOutputStream(res_file, true);
        } catch(Exception e) {
            System.out.println("sth wrong happen 3");
        }
        osw = new OutputStreamWriter(fos);
        bw = new BufferedWriter(osw);
    }

    public Site getSite() {
        return site;
    }

    public void process(Page page) {
        if(!load) {
            try {
                user_num = LoadUrls("fanfan-ref.txt", "fanfan-2020-05-16.txt");
            } catch(Exception e) {
                System.out.println("sth wrong happen 1");
            }
            load = true;
            System.out.println("urls num = " + String.valueOf(uids.length));
            List<String> tmp_urls = new ArrayList<String>();
            for(int i = 0; i < uids.length; ++i) {
                String tmp_url = genFanUrl(uids[i], String.valueOf(0));
                tmp_urls.add(tmp_url);
            }
            page.addTargetRequests(tmp_urls);
            return;
        }

        String url = page.getUrl().toString();
        String uid = genUid(url);
        String pgid = genPgid(url);
//        String maxpg = page.getJson().jsonPath("$.data.maxPage").toString();

        if(pgid.equals("0")) {
            List<String> tmp_urls = new ArrayList<String>();
            String maxpg = page.getJson().jsonPath("$.data.maxPage").toString();
            Integer pg = Integer.parseInt(maxpg) > 10 ? 10 : Integer.parseInt(maxpg);
            for(int i = 1; i < pg; ++i) {
                String tmp_url = genFanUrl(uid, String.valueOf(i));
                tmp_urls.add(tmp_url);
            }
            page.addTargetRequests(tmp_urls);
        }

        Integer fanfan = 0;

        List<String> fans = page.getJson().jsonPath("$.data.cards[*].user.followers_count").all();

        for(String fan : fans) {
            fanfan += Integer.parseInt(fan);
        }

        System.out.println("fan : " + String.valueOf(fanfan) + " total_solve : " + String.valueOf(total_solve));

        try {
            bw.write(uid + " " + fanfan.toString() + " " + pgid + "\n");
            bw.flush();
            total_solve++;
        } catch(Exception e) {
            System.out.println("sth wrong happen 2");
        }


    }

    public static void main(String[] args) {
        WeiboFanfanProcessor weiboProcessor = new WeiboFanfanProcessor();
        Spider weibo = Spider.create(weiboProcessor)
                .addUrl("https://m.weibo.cn/api/container/getSecond?containerid=1005051680899465_-_FOLLOWERS&page=0");
        weibo.start();
    }

    private Integer LoadUrls(String filename, String al_filename) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(filename);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        int n = Integer.parseInt(br.readLine());
        uids = new String[n];
        for(int i = 0; i < n; ++i) {
            String uid = br.readLine();
            uids[i] = uid;
            uidmap.add(uid);
//            urls.add(genFollowUrl(uid, String.valueOf(0)));
//            urls.add(genFanUrl(uid, String.valueOf(0)));
        }

        File file = new File(al_filename);
        if(file.exists()) {
            fis = new FileInputStream(al_filename);
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
            while(true) {
                String tmp = br.readLine();
                if(tmp == null || tmp.length() == 0) break;
                String uid = tmp.split(" ")[0];
                al_uidmap.add(uid);
                total_solve += 1;
            }
        }
        return n;
    }

    private String genFanUrl(String uid, String pgid) {
        StringBuilder res = new StringBuilder(128);
        res.append("https://m.weibo.cn/api/container/getSecond?containerid=100505");
        res.append(uid);
        res.append("_-_FANS&page=");
        res.append(pgid);
        return res.toString();
    }

    private String genUid(String url) {
        Integer beg = url.indexOf("containerid=100505");
        Integer end = url.indexOf("_-_");
        if(beg + 18 >= end) return null;
        return url.substring(beg + 18, end);
    }

    private String genPgid(String url) {
        Integer index = url.indexOf("page=");
        if(index != -1) {
            return url.substring(index + 5);
        }
        return null;
    }

}
