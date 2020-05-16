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

public class WeiboRandomProcessor implements PageProcessor{
    private Site site = Site.me().setRetryTimes(5).setSleepTime(2500);

    WeiboRandomProcessor() {

    }

    public Site getSite() {
        return site;
    }

    public void process(Page page) {

    }

    public static void main(String[] args) {
        WeiboRandomProcessor weiboProcessor = new WeiboRandomProcessor();
        Spider weibo = Spider.create(weiboProcessor)
                .addUrl("https://m.weibo.cn/api/container/getSecond?containerid=1005051680899465_-_FOLLOWERS&page=0");
        weibo.start();
    }
}
