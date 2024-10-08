package searchengine.model;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LemmaCreator {
    public Map<String, Integer> getLemmas(String text) {
        Map<String, Integer> lemmas = new HashMap<>();
        try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();

            String[] words = takeWordsFromText(text);

            for (String word : words) {
                if (word.isEmpty()) continue;
                List<String> wordMorphInfo = luceneMorph.getMorphInfo(word);
                wordMorphInfo = wordMorphInfo.stream()
                        .filter(w -> !w.contains("СОЮЗ") &&
                                !w.contains("ПРЕДЛ") &&
                                !w.contains("ЧАСТ") &&
                                !w.contains("МЕЖД"))
                        .toList();

                if (!wordMorphInfo.isEmpty()) {
                    String lemma = takeLemmaFromWord(word, luceneMorph);
                    if (lemma != null) {
                        int wordCount = lemmas.containsKey(lemma) ? lemmas.get(lemma) + 1 : 1;
                        lemmas.put(lemma, wordCount);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return lemmas;
    }

    public String[] takeWordsFromText(String text) {
        text = text.toLowerCase();

        String regex = "[^а-я]";
        return text.replaceAll(regex, " ")
                .split("\\s+");
    }


    public String takeLemmaFromWord(String word,
                                    LuceneMorphology luceneMorph) {
        List<String> wordBaseForms = luceneMorph.getNormalForms(word);
        if (!wordBaseForms.isEmpty()) {
            return wordBaseForms.get(0);
        }
        return null;
    }
}
