package com.teampinguins.essentialoils.api.services;

import java.util.ArrayList;

public class TanimotoSimilarity {

    private final int stl;
    private final int mwl;
    private final double thw;

    public TanimotoSimilarity(int subTokenLength, int minWordLength, double thresholdWord) {
        stl = subTokenLength;
        mwl = minWordLength;
        thw = thresholdWord;
    }

    private String NormalizeSentence(String sentence) {
        var resultContainer = new StringBuilder(100);
        CharSequence lowerSentece = sentence.toLowerCase();
        for (int i = 0; i < lowerSentece.length(); i++) {
            if (IsNormalChar(lowerSentece.charAt(i))) {
                resultContainer.append(lowerSentece.charAt(i));
            }
        }

        return resultContainer.toString();
    }

    private boolean IsNormalChar(char c) {
        return Character.isLetterOrDigit(c) || c == ' ';
    }

    private ArrayList<String> GetTokens(String sentence) {
        var tokens = new ArrayList<String>();
        var words = sentence.split(" ");
        for (String word : words) {
            if (word.length() >= mwl) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private boolean IsTokensFuzzyEqual(String firstToken, String secondToken) {
        int equalSubTokensCount = 0;
        if (secondToken.length() - stl + 1 <= 0) {
            return false;
        }
        boolean[] usedTokens = new boolean[secondToken.length() - stl + 1];
        for (var i = 0; i < firstToken.length() - stl + 1 && i <= stl; i++) {
            var subTokenFirst = firstToken.substring(i, stl);
            for (var j = 0; j < secondToken.length() - stl + 1 && j <= stl; j++) {
                if (!usedTokens[j]) {
                    var subTokenSecond = secondToken.substring(j, stl);
                    if (subTokenFirst.equals(subTokenSecond)) {
                        equalSubTokensCount++;
                        usedTokens[j] = true;
                        break;
                    }
                }
            }
        }

        var subTokenFirstCount = firstToken.length() - stl + 1;
        var subTokenSecondCount = secondToken.length() - stl + 1;
        var tanimoto = (1.0 * equalSubTokensCount) / (subTokenFirstCount + subTokenSecondCount - equalSubTokensCount);

        return thw <= tanimoto;
    }

    public double CalculateFuzzyEqualValue(String first, String second) {
        if (first.trim().isEmpty() && second.trim().isEmpty()) {
            return 1.0;
        }

        if (first.trim().isEmpty() || second.trim().isEmpty()) {
            return 0.0;
        }

        var normalizeStringFirst = NormalizeSentence(first);
        var normalizeStringSecond = NormalizeSentence(second);

        var tokensFirst = GetTokens(normalizeStringFirst);
        var tokensSecond = GetTokens(normalizeStringSecond);

        var fuzzyEqualsTokens = GetFuzzyEqualsTokens(tokensFirst, tokensSecond);

        var equalsCount = fuzzyEqualsTokens.size();
        var firstCount = tokensFirst.size();
        var secondCount = tokensSecond.size();

        return (1.0 * equalsCount) / (firstCount + secondCount - equalsCount);
    }

    private ArrayList<String> GetFuzzyEqualsTokens(ArrayList<String> tokensFirst, ArrayList<String> tokensSecond) {
        var equalsTokens = new ArrayList<String>();
        var usedToken = new boolean[tokensSecond.size()];
        for (String s : tokensFirst) {
            for (var j = 0; j < tokensSecond.size(); ++j) {
                if (!usedToken[j]) {
                    if (IsTokensFuzzyEqual(s, tokensSecond.get(j))) {
                        equalsTokens.add(s);
                        usedToken[j] = true;
                        break;
                    }
                }
            }
        }

        return equalsTokens;
    }
}
