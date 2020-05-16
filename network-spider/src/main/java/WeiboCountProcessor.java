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

    private Site site = Site.me().setRetryTimes(5).setSleepTime(2500);
//    private Site site = Site.me().setRetryTimes(5).setSleepTime(2500).setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac" +
//        " OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.113 Safari/537.36");
//    private Site site = Site.me().setRetryTimes(5).setSleepTime(2000).setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0");

    private boolean load;
    private boolean record;
    private Integer total_solve;
    private String[] uids;
    private Integer user_num;
//    List<String> urls;
    private Integer cur_pos;
    private Integer fanfan_num;

    private String[] ua = {"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36 OPR/37.0.2178.32",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.57.2 (KHTML, like Gecko) Version/5.1.7 Safari/534.57.2",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586",
            "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko",
            "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0)",
            "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)",
            "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0)",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 BIDUBrowser/8.3 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.80 Safari/537.36 Core/1.47.277.400 QQBrowser/9.4.7658.400",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 UBrowser/5.6.12150.8 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.122 Safari/537.36 SE 2.X MetaSr 1.0",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36 TheWorld 7",
            "Mozilla/5.0 (Windows NT 6.1; W…) Gecko/20100101 Firefox/60.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.113 Safari/537.36"};

    private HashSet<String> uidmap;
    private HashSet<String> al_uidmap;

    private FileOutputStream fos;
    private OutputStreamWriter osw;
    private BufferedWriter bw;

    private HashMap<String, String> uid2follow;
    private HashMap<String, String> uid2fan;
    private HashMap<String, Integer> uid2fanfan;

    WeiboCountProcessor() {
        load = false;
        record = false;
        total_solve = 0;
        user_num = 0;
        cur_pos = 0;
        fanfan_num = 0;
//        urls = new ArrayList<String>();
        uidmap = new HashSet<String>();
        al_uidmap = new HashSet<String>();
        uid2follow = new HashMap<String, String>();
        uid2fan = new HashMap<String, String>();
        uid2fanfan = new HashMap<String, Integer>();

        String res_file = "res-2020-05-16.txt";
        try {
            fos = new FileOutputStream(res_file, true);
        } catch(Exception e) {
            System.out.println("sth wrong happen");
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
                user_num = LoadUrls("user-ref.txt", "res-2020-05-16.txt");
            } catch (Exception e) {
                System.out.println("cannot open file!");
            }
            load = true;
            System.out.println("urls num = " + String.valueOf(uids.length));
            //page.addTargetRequests(urls);
            //return;
//            List<String> tmp_urls = new ArrayList<String>();
//            tmp_urls.add("https://m.weibo.cn/api/container/getSecond?containerid=1005051680899465_-_FANS&page=0");
//            page.addTargetRequests(tmp_urls);
        }

//        System.out.println("user_num = " + String.valueOf(user_num));

        String url = page.getUrl().toString();
        String uid = genUid(url);
        String pgid = genPgid(url);

//        if(al_uidmap.contains(uid)) return;

        List<String> ids = page.getJson().jsonPath("$.data.cards[*].user.id").all();
        List<String> fans = page.getJson().jsonPath("$.data.cards[*].user.followers_count").all();
        List<String> followers = page.getJson().jsonPath("$.data.cards[*].user.follow_count").all();
        if(Integer.parseInt(pgid) != 0) {
            Integer tmp_num = 0;
            for(int i = 0; i < ids.size(); ++i) {
                tmp_num += Integer.parseInt(fans.get(i));
            }
            uid2fanfan.put(uid, uid2fanfan.get(uid) + tmp_num);
            return;
        }


        if(judgeFollow(url)) { //follower page
            if(!al_uidmap.contains(uid) && !uid2follow.containsKey(uid)) {
                String follow = page.getJson().jsonPath("$.data.count").toString();
                uid2follow.put(uid, follow);
                total_solve++;
            }
        } else { //fan page
            if(!al_uidmap.contains(uid) && !uid2fan.containsKey(uid)) {
                String fan = page.getJson().jsonPath("$.data.count").toString();
                uid2fan.put(uid, fan);
//                if(fanfan_num < 300) {
//                    uid2fanfan.put(uid, 0);
//                    Integer tmp_num = 0;
//                    for(int i = 0; i < ids.size(); ++i) {
//                        tmp_num += Integer.parseInt(fans.get(i));
//                    }
//                    uid2fanfan.put(uid, uid2fanfan.get(uid) + tmp_num);
//                    fanfan_num++;
//                    String maxpg = page.getJson().jsonPath("$.data.maxPage").toString();
//                    Integer bound = Integer.parseInt(maxpg);
//                    if(bound > 10) bound = 10;
//                    List<String> tmp_urls = new ArrayList<String>();
//                    for(int i = 1; i < bound; ++i) {
//                        tmp_urls.add(genFanUrl(uid, String.valueOf(i)));
//                    }
//                    page.addTargetRequests(tmp_urls);
//                }
                total_solve++;
                try {
                    bw.write(uid + " " + uid2follow.get(uid) + " " + uid2fan.get(uid) + "\n");
                    bw.flush();
                } catch(Exception e) {
                    System.out.println("cannot write");
                }
            }

        }


        for(int i = 0; i < ids.size(); ++i) {
            Boolean tmpb = false;
            if(!al_uidmap.contains(ids.get(i))) {
                if (uidmap.contains(ids.get(i)) && !uid2follow.containsKey(ids.get(i))) {
                    uid2follow.put(ids.get(i), followers.get(i));
                    //                total_solve++;
                }
                if (uidmap.contains(ids.get(i)) && !uid2fan.containsKey(ids.get(i))) {
                    uid2fan.put(ids.get(i), fans.get(i));
                    //                total_solve++;
                    tmpb = true;
                }
                if (tmpb && uidmap.contains(ids.get(i))) {
                    try {
                        bw.write(ids.get(i) + " " + uid2follow.get(ids.get(i)) + " " + uid2fan.get(ids.get(i)) + "\n");
                        total_solve += 2;
                    } catch (Exception e) {
                        System.out.println("cannot write");
                    }
                }
            }
        }

        System.out.println(total_solve);
//        total_solve++;

        while(cur_pos < uids.length) {
            if(!al_uidmap.contains(uids[cur_pos]) && !uid2follow.containsKey(uids[cur_pos])) {
                ArrayList<String> tmp_urls = new ArrayList<String>();
                tmp_urls.add(genFollowUrl(uids[cur_pos], String.valueOf(0)));
                tmp_urls.add(genFanUrl(uids[cur_pos], String.valueOf(0)));
                page.addTargetRequests(tmp_urls);
                cur_pos++;
                break;
            }
//            else if (cur_pos == user_num - 1) {
//                ArrayList<String> tmp_urls = new ArrayList<String>();
//                tmp_urls.add(genFanUrl(uids[cur_pos], String.valueOf(0)));
//                page.addTargetRequests(tmp_urls);
//            }
            else cur_pos++;
        }

//        if(page.getTargetRequests().size() == 0) {
//            if(!record) {
//                try {
//                    RecordData();
//                } catch(Exception e) {
//                    System.out.println("sth wrong happen");
//                }
//            }
//            record = true;
//            return;
//        }

    }

    public static void main(String[] args) {
        WeiboCountProcessor weiboProcessor = new WeiboCountProcessor();
        Spider weibo = Spider.create(weiboProcessor)
                .addUrl("https://m.weibo.cn/api/container/getSecond?containerid=1005051680899465_-_FOLLOWERS&page=0");
        weibo.start();
    }

//    private void RecordData() throws Exception {
//        Date date = new Date();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        String cur_time =  sdf.format(date);
//        String res_file = "res-" + cur_time + ".txt";
//        FileOutputStream fos = new FileOutputStream(res_file, true);
//        OutputStreamWriter osw = new OutputStreamWriter(fos);
//        BufferedWriter bw = new BufferedWriter(osw);
//        for(int i = 0; i < uids.length; ++i) {
//            String follow = (uid2follow.containsKey(uids[i])) ? uid2follow.get(uids[i]) : String.valueOf(-1);
//            String fan = (uid2fan.containsKey(uids[i])) ? uid2fan.get(uids[i]) : String.valueOf(-1);
//            String fanfan = (uid2fanfan.containsKey(uids[i])) ? String.valueOf(uid2fanfan.get(uids[i])) : String
//                    .valueOf(-1);
//            bw.write(uids[i] + " " + follow + " " + fan + " " + fanfan +  "\n");
//        }
//        bw.flush();
//        bw.close();
//    }

//    private void RecordData() throws Exception {
//
//    }

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
                total_solve += 2;
            }
        }


        return n;
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

    private String genPgid(String url) {
        Integer index = url.indexOf("page=");
        if(index != -1) {
            return url.substring(index + 5);
        }
        return null;
    }
}
