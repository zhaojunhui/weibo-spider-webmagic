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

public class WeiboCountProcessor implements PageProcessor {

    private Site site = Site.me().setRetryTimes(5).setSleepTime(1500);

    private boolean load;
    private boolean record;
    private Integer total_solve;
    private String[] uids;
    private Integer user_num;
    List<String> urls;

    private HashMap<String, String> uid2follow;
    private HashMap<String, String> uid2fan;
    private HashMap<String, Integer> uid2fanfan;

    WeiboCountProcessor() {
        load = false;
        record = false;
        total_solve = 0;
        user_num = 0;
        urls = new ArrayList<String>();
        uid2follow = new HashMap<String, String>();
        uid2fan = new HashMap<String, String>();
        uid2fanfan = new HashMap<String, Integer>();
    }

    public Site getSite() {
        return site;
    }

    public void process(Page page) {
        if(load == false) {
            try {
                user_num = LoadUrls("user-ref.txt");
            } catch (Exception e) {
                System.out.println("cannot open file!");
            }
            load = true;
            System.out.println("urls num = " + String.valueOf(urls.size()));
            page.addTargetRequests(urls);
            return;
        }

        System.out.println("user_num = " + String.valueOf(user_num));

        String url = page.getUrl().toString();
        String uid = genUid(url);
        if(judgeFollow(url)) { //follower page
            if(!uid2follow.containsKey(uid)) {
                String follow = page.getJson().jsonPath("$.data.count").toString();
                uid2follow.put(uid, follow);
//                total_solve++;
            }
        } else { //fan page
            if(!uid2fan.containsKey(uid)) {
                String fan = page.getJson().jsonPath("$.data.count").toString();
                uid2fan.put(uid, fan);
                uid2fanfan.put(uid, 0);
//                total_solve++;
            }
            List<String> fanfans = page.getJson().jsonPath("$.data.cards[*].user.followers_count").all();
            Integer total_fanfan = 0;
            for (String fanfan : fanfans) {
                total_fanfan += Integer.parseInt(fanfan);
            }
            uid2fanfan.put(uid, uid2fanfan.get(uid) + total_fanfan);
        }

        total_solve++;
        if(total_solve == 2 * user_num) {
            if(!record) {
                try {
                    RecordData();
                } catch(Exception e) {
                    System.out.println("sth wrong happen");
                }
            }
            record = true;
            return;
        }

    }

    public static void main(String[] args) {
        WeiboCountProcessor weiboProcessor = new WeiboCountProcessor();
        Spider weibo = Spider.create(weiboProcessor)
                .addUrl("https://m.weibo.cn/api/container/getSecond?containerid=1005053261134774_-_FOLLOWERS&page=0");
        weibo.start();
    }

    private void RecordData() throws Exception {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String cur_time =  sdf.format(date);
        String res_file = "res-" + cur_time + ".txt";
        FileOutputStream fos = new FileOutputStream(res_file);
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        BufferedWriter bw = new BufferedWriter(osw);
        for(int i = 0; i < uids.length; ++i) {
            String follow = (uid2follow.containsKey(uids[i])) ? uid2follow.get(uids[i]) : String.valueOf(-1);
            String fan = (uid2fan.containsKey(uids[i])) ? uid2fan.get(uids[i]) : String.valueOf(-1);
            String fanfan = (uid2fanfan.containsKey(uids[i])) ? String.valueOf(uid2fanfan.get(uids[i])) : String
                    .valueOf(-1);
            bw.write(uids[i] + " " + follow + " " + fan + " " + fanfan +  "\n");
        }
        bw.flush();
        bw.close();
    }

    private Integer LoadUrls(String filename) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(filename);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        int n = Integer.parseInt(br.readLine());
        uids = new String[n-90];
        for(int i = 0; i < n-90; ++i) {
            String uid = br.readLine();
            uids[i] = uid;
            urls.add(genFollowUrl(uid, String.valueOf(0)));
            urls.add(genFanUrl(uid, String.valueOf(0)));
        }
        return n-90;
    }

    private boolean judgeFollow(String url) {
        if(url.indexOf("FOLLOWER") != -1)
            return true;
        else return false;
    }

    private String genFollowUrl(String uid, String pgid) {
        StringBuilder res = new StringBuilder(128);
        res.append("https://m.weibo.cn/api/container/getSecond?containerid=100505");
        res.append(uid);
        res.append("_-_FOLLOWERS&page=");
        res.append(pgid);
        return res.toString();
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
}
