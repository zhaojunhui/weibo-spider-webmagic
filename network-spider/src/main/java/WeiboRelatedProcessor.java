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
import java.util.HashSet;
import java.util.List;
import java.io.*;
import java.util.HashMap;

public class WeiboRelatedProcessor implements PageProcessor {

    private Site site = Site.me().setRetryTimes(5).setSleepTime(1500);
    HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
    private String[] proxys = {"152.136.96.214", "182.46.203.166",
                            "111.229.224.145", "218.27.136.169",
                            "18.163.28.22", "124.156.98.172"};
    private Integer[] ports = {8080, 9999, 8118, 8086, 1080, 443};

    private HashSet<String> uids;
    private Integer maxuser;

    private boolean record = false;

//    private HashMap<String, String[]> global_res;


//    @Override
    WeiboRelatedProcessor() {
        uids = new HashSet<String>();
//        global_uids = new HashSet<String>();
        maxuser = 0;
        httpClientDownloader.setProxyProvider(SimpleProxyProvider.from(
                new Proxy(proxys[0], ports[0]),
                new Proxy(proxys[1], ports[1]),
                new Proxy(proxys[2], ports[2]),
                new Proxy(proxys[3], ports[3]),
                new Proxy(proxys[4], ports[4]),
                new Proxy(proxys[5], ports[5])
                ));
    }

//    @Override
    public void process(Page page) {
//        System.out.println(page.getUrl().toString());
        String uid = genUid(page.getUrl().toString());
        String pgid = genPgid(page.getUrl().toString());
//        System.out.println(uid + "    " + pgid);
//
//        System.out.println(uid + "    " + maxuser);

        if(uid == null || pgid == null) {
            page.setSkip(true);
            return;
        }

        if(maxuser >= 100) {
            if(!record) {
                try {
                    recordData();
                } catch(Exception e) {
                    System.out.println("sth wrong happen!");
                }
                record = true;
            }
            page.setSkip(true);
            return;
        }



        List<String> urls = new ArrayList<String>();

        if(!uids.contains(uid)) {
            uids.add(uid);
            maxuser++;
        }
        if(Integer.parseInt(pgid) == 0) {
            String maxid = page.getJson().jsonPath("$.data.maxPage").toString();
//            System.out.println(maxid);
            Integer bound = Integer.parseInt(maxid);
            if(bound > 5000) bound = 5000;
            for(int i = 1; i <= bound; i++) {
                urls.add(genUrl(uid, String.valueOf(i)));
            }
        }

//        List<String> relateids = page.getJson().jsonPath("$data.cards[*].user.id").all();
        for(String rid : page.getJson().jsonPath("$.data.cards[*].user.id").all()) {
            if(!uids.contains(rid)) {
                uids.add(rid);
                urls.add(genUrl(rid, String.valueOf(0)));
                maxuser++;
            }
        }

        //System.out.println(uids.size());
        page.addTargetRequests(urls);

    }

//    @Override
    public Site getSite() {
        return site;
    }

    public HttpClientDownloader getHttpClientDownloader() {return httpClientDownloader; }

    public static void main(String[] args) throws Exception {

        WeiboRelatedProcessor weiboProcessor = new WeiboRelatedProcessor();
        Spider weibo = Spider.create(weiboProcessor)
//                .setDownloader(weiboProcessor.getHttpClientDownloader())
                .addUrl("https://m.weibo.cn/api/container/getSecond?containerid=1005053261134763_-_FOLLOWERS&page=0");

        SpiderMonitor.instance().register(weibo);
        weibo.start();
    }

    private void recordData() throws Exception {
        System.out.println(uids.size());
        FileOutputStream fos = new FileOutputStream("user-ref.txt");
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(String.valueOf(uids.size()) + "\n");
        for(String uid : uids) {
            bw.write(uid + "\n");
        }
        bw.flush();
        bw.close();
    }

    private String genUrl(String uid, String pgid) {
        StringBuilder res = new StringBuilder(128);
        res.append("https://m.weibo.cn/api/container/getSecond?containerid=100505");
        res.append(uid);
        res.append("_-_FOLLOWERS&page=");
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
