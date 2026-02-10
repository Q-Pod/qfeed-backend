package com.ktb.fixture;

import com.ktb.hashtag.domain.Hashtag;
import com.ktb.hashtag.domain.QuestionHashtag;
import com.ktb.question.domain.Question;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuestionHashtagFixture {

    public static QuestionHashtag createQuestionHashtag() {
        return QuestionHashtag.create(
                mock(Question.class),
                HashtagFixture.createHashtag()
        );
    }

    public static QuestionHashtag createQuestionHashtag(Question question, Hashtag hashtag) {
        return QuestionHashtag.create(question, hashtag);
    }

    public static QuestionHashtag createMockQuestionHashtag(Long hashtagId, String keyword) {
        QuestionHashtag questionHashtag = mock(QuestionHashtag.class);
        Hashtag hashtag = mock(Hashtag.class);

        when(hashtag.getId()).thenReturn(hashtagId);
        when(hashtag.getName()).thenReturn(keyword.toLowerCase());
        when(questionHashtag.getHashtag()).thenReturn(hashtag);

        return questionHashtag;
    }

    public static List<QuestionHashtag> createMockQuestionHashtags(String... keywords) {
        return Arrays.stream(keywords)
                .map(keyword -> {
                    long id = (long) keyword.hashCode();
                    return createMockQuestionHashtag(id, keyword);
                })
                .toList();
    }

    public static List<QuestionHashtag> createQuestionHashtagsWithKeywords(Long questionId, String... keywords) {
        Question question = mock(Question.class);
        when(question.getId()).thenReturn(questionId);

        return Arrays.stream(keywords)
                .map(keyword -> {
                    Hashtag hashtag = HashtagFixture.createHashtag(keyword);
                    return QuestionHashtag.create(question, hashtag);
                })
                .toList();
    }

    public static List<QuestionHashtag> createEmptyQuestionHashtags() {
        return List.of();
    }
}
