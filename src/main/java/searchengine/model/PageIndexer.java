package searchengine.model;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.UserSettings;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;


@RequiredArgsConstructor
public class PageIndexer extends RecursiveTask<Void> {
    private final SiteEntity site;
    private final String path;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final UserSettings userSettings;
    private final AtomicBoolean running;
    private final List<PageIndexer> tasks = new ArrayList<>();

    @Override
    protected Void compute() {
        try {
            if (!running.get()) return null;

            Document doc = getDoc();
            if (doc == null) return null;

            createPageWithLemmasAndIndices(doc);

            Elements links = doc.select("a");

            for (Element link : links) {
                String url = link.absUrl("href");
                PageEntity page;
                page = pageRepository.findByPath(url);
                if (!url.equals(path) &&
                        url.contains(path) &&
                        !url.contains("#") &&
//             TODO  Следующую строку нужно удалить, чтобы выдавал все страницы
//                        !url.contains("html") &&
//                        !url.contains("institute") &&
//                        !url.contains("scie") &&
//                        !url.contains("train") &&
//                        !url.contains("identit") &&
//                        !url.contains("seminars") &&
//                        !url.contains("publi") &&
                        page == null) {
                    PageIndexer task = new PageIndexer(site, url,
                            siteRepository, pageRepository,
                            lemmaRepository, indexRepository,
                            userSettings, running);
                    task.fork();
                    tasks.add(task);
                }
            }
            invokeAll(tasks);
        } catch (Exception e) {
            System.out.println("Ошибка compute: " + e.getMessage() + " для страницы " + path);
        }
        for (PageIndexer task : tasks) task.join();
        return null;
    }


    public Document getDoc() {
        try {
            Thread.sleep(150);
            return Jsoup.connect(path)
                    .userAgent(userSettings.getUser())
                    .referrer(userSettings.getReferrer())
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .get();
        } catch (Exception e) {
            System.out.println("Ошибка getDoc: " + e.getMessage() + " для страницы " + path);
        }
        return null;
    }

    public void createPageWithLemmasAndIndices(Document doc) {
        updateStatusTime();
        PageEntity page = createPage(doc);
        if (page != null
                && page.getCode() < 400)
            createLemmasAndIndices(page);
    }

    @Transactional
    public void updateStatusTime() {
        if (running.get()) {
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
    }

    @Transactional
    public PageEntity createPage(Document doc) {
        try {
            PageEntity page = new PageEntity();
            page.setSite(site);
            page.setPath(path);
            page.setContent(doc.html());
            page.setCode(doc.connection().response().statusCode());
            synchronized (pageRepository) {
                if (pageRepository.findByPath(path) == null) {
                    pageRepository.save(page);
                    System.out.println("Создание страницы " + path);
                    return page;
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка createPage: " + e.getMessage() + " для страницы " + path);
            return null;
        }
        return null;
    }

    @Transactional
    public void createLemmasAndIndices(PageEntity page) {
        try {
            String pageContent = page.getContent();
            SiteEntity site = page.getSite();
            LemmaCreator lemmaCreator = new LemmaCreator();
            Map<String, Integer> lemmas = lemmaCreator.getLemmas(pageContent);

            List<IndexEntity> indicesToSave = new ArrayList<>();

            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                String lemmaText = entry.getKey();
                float count = entry.getValue();
                LemmaEntity lemma;
                synchronized (lemmaRepository) {
                    lemma = lemmaRepository.findByLemmaAndSiteId(lemmaText, site);
                    if (lemma == null) {
                        lemma = new LemmaEntity();
                        lemma.setSite(site);
                        lemma.setLemma(lemmaText);
                        lemma.setFrequency(1);
                    } else {
                        lemma.setFrequency(lemma.getFrequency() + 1);
                    }


                    lemmaRepository.save(lemma);
                }
                IndexEntity index;
                index = indexRepository.findByLemmaIdAndPageId(lemma, page);
                if (index == null) {
                    index = new IndexEntity();
                    index.setPage(page);
                    index.setLemma(lemma);
                    index.setRank(count);
                    indicesToSave.add(index);
                }
            }
            indexRepository.saveAll(indicesToSave);

        } catch (Exception e) {
            System.out.println("Ошибка createLemma: " + e.getMessage());
        }
    }
}