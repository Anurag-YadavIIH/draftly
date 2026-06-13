package com.airtribe.draftly.service.rag;

import com.airtribe.draftly.config.AppProperties;
import com.airtribe.draftly.domain.StyleSample;
import com.airtribe.draftly.repository.StyleSampleRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * The "R" in RAG. Given the text of an incoming email, this finds the user's
 * most stylistically relevant past sent emails so the LLM can mimic their tone
 * and phrasing.
 *
 * Pipeline: embed the query -> load the user's stored sample embeddings ->
 * rank by cosine similarity -> return the top-K sample texts.
 */
@Service
public class StyleRetriever {

    private final StyleSampleRepository sampleRepository;
    private final EmbeddingClient embeddingClient;
    private final AppProperties props;

    public StyleRetriever(StyleSampleRepository sampleRepository,
                          EmbeddingClient embeddingClient,
                          AppProperties props) {
        this.sampleRepository = sampleRepository;
        this.embeddingClient = embeddingClient;
        this.props = props;
    }

    /**
     * Store one of the user's sent emails as a style sample, embedding it so it
     * can be retrieved later.
     */
    public StyleSample indexSentEmail(String userEmail, String text) {
        double[] embedding = embeddingClient.embed(text);
        StyleSample sample = new StyleSample(userEmail, text, VectorMath.toCsv(embedding));
        return sampleRepository.save(sample);
    }

    /**
     * @return up to {@code draftly.rag-top-k} of the user's past emails, most
     *         stylistically similar to {@code query} first.
     */
    public List<String> retrieveSimilar(String userEmail, String query) {
        double[] queryVec = embeddingClient.embed(query);
        List<StyleSample> samples = sampleRepository.findByUserEmail(userEmail);

        return samples.stream()
                .sorted(Comparator.comparingDouble(
                        (StyleSample s) -> VectorMath.cosineSimilarity(
                                queryVec, VectorMath.fromCsv(s.getEmbedding())))
                        .reversed())
                .limit(props.getRagTopK())
                .map(StyleSample::getText)
                .toList();
    }
}
