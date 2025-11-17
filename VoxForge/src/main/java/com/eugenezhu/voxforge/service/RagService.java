package com.eugenezhu.voxforge.service;

import com.eugenezhu.voxforge.config.AiConfig;
import com.eugenezhu.voxforge.model.CommandTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @projectName: VoxForge
 * @package: com.eugenezhu.voxforge.service
 * @className: RagService
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/11/17 上午11:58
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    @Qualifier("llmWebClient")
    private final WebClient llmWebClient;

    private final AiConfig.LlmProperties llmProperties;

    private final Map<CommandTemplate, double[]> vectors = new ConcurrentHashMap<>();

    /**
     * 命令库
     */
    private final List<CommandTemplate> library = new ArrayList<CommandTemplate>(
            List.of(
                    new CommandTemplate("start notepad.exe", "打开记事本", "Windows 11", "cmd"),
                    new CommandTemplate("start calc.exe", "打开计算器", "Windows 11", "cmd"),
                    new CommandTemplate("start chrome", "打开浏览器", "Windows 11", "cmd"),
                    new CommandTemplate("explorer", "打开资源管理器", "Windows 11", "cmd"),
                    new CommandTemplate("dir", "列出目录", "Windows 11", "cmd"),
                    new CommandTemplate("ls -la", "列出目录详细", "Ubuntu 22.04", "bash"),
                    new CommandTemplate("xdg-open .", "打开文件管理器", "Ubuntu 22.04", "bash"),
                    new CommandTemplate("google-chrome", "打开浏览器", "Ubuntu 22.04", "bash"),
                    new CommandTemplate("nano", "打开文本编辑器", "Ubuntu 22.04", "bash"),
                    new CommandTemplate("code", "打开VS Code", "Windows 11", "cmd")
            )
    );

    /**
     * 从命令库中检索与用户输入最相关的命令
     * @param text 用户输入的文本
     * @param env 环境变量，包含用户操作系统信息
     * @param k 返回的命令数量
     * @return 与用户输入最相关的命令列表
     */
    public List<CommandTemplate> retrieve(String text, Map<String, Object> env, int k) {
        String os = env.get("os").toString();
        List<CommandTemplate> candidates = library.stream()
                .filter(cmd -> cmd.getOs().equals(os))
                .toList();
        ensureTemplateVectors(candidates);
        double[] q = embedBlocking(text);
        if (q == null) {
            return fallbackTfRetrieve(text, candidates, k);
        }

        return candidates.stream()
                .map(t -> Map.entry(t, cosine(q, vectors.get(t))))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(k)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * 确保命令模板的向量存在
     * @param candidates 命令模板列表
     */
    private void ensureTemplateVectors(List<CommandTemplate> candidates) {
        List<CommandTemplate> missing = candidates.stream().filter(t -> !vectors.containsKey(t)).toList();
        if (missing.isEmpty()) return;
        for (CommandTemplate t : missing) {
            double[] v = embedBlocking(t.getCmd() + " " + t.getDesc());
            if (v != null) vectors.put(t, v);
        }
    }

    /**
     * 调用LLM API生成文本的嵌入向量
     * @param text 输入文本
     * @return 嵌入向量数组，或null如果失败
     */
    private double[] embedBlocking(String text) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("input", text);
            body.put("model", llmProperties.getModel());
            Map<String, Object> resp = llmWebClient.post()
                    .uri("/embeddings")
                    .header("Authorization", "Bearer " + llmProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_NDJSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .onErrorReturn(Collections.emptyMap())
                    .blockOptional()
                    .orElse(Collections.emptyMap());

            Object data = resp.get("data");
            if (data instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?,?> m) {
                    Object emb = m.get("embedding");
                    if (emb instanceof List<?> vec) {
                        double[] arr = new double[vec.size()];
                        for (int i = 0; i < vec.size(); i++) {
                            Object x = vec.get(i);
                            arr[i] = x instanceof Number ? ((Number) x).doubleValue() : 0d;
                        }
                        return normalize(arr);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算两个向量的余弦相似度
     * @param a 第一个向量
     * @param b 第二个向量
     * @return 余弦相似度值，范围在[0, 1]之间
     */
    private double cosine(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        if (denom == 0) return 0;
        return dot / denom;
    }

    /**
     * 对向量进行归一化，将其转换为单位向量
     * @param v 输入向量
     * @return 归一化后的向量
     */
    private double[] normalize(double[] v) {
        double n = 0;
        for (double x : v) n += x * x;
        n = Math.sqrt(n);
        if (n == 0) return v;
        double[] r = new double[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i] / n;
        return r;
    }

    /**
     * 基于词频向量计算余弦相似度，作为回退检索方法
     * @param text 查询文本
     * @param candidates 候选命令模板列表
     * @param k 返回的Top-K结果数量
     * @return 基于余弦相似度排序的Top-K命令模板列表
     */
    private List<CommandTemplate> fallbackTfRetrieve(String text, List<CommandTemplate> candidates, int k) {
        Map<CommandTemplate, Double> scores = new HashMap<>();
        Map<String, Integer> va = tf(text);
        for (CommandTemplate t : candidates) {
            Map<String, Integer> vb = tf(t.getCmd() + " " + t.getDesc());
            Set<String> keys = new HashSet<>(); keys.addAll(va.keySet()); keys.addAll(vb.keySet());
            double dot = 0, na = 0, nb = 0; for (String key : keys) { int xa = va.getOrDefault(key, 0); int xb = vb.getOrDefault(key, 0); dot += xa*xb; na += xa*xa; nb += xb*xb; }
            double denom = Math.sqrt(na) * Math.sqrt(nb); double sim = denom==0?0:dot/denom; scores.put(t, sim);
        }
        return scores.entrySet().stream().sorted((a,b)->Double.compare(b.getValue(), a.getValue())).limit(k).map(Map.Entry::getKey).toList();
    }

    /**
     * 计算字符串的词频向量
     * @param s 输入字符串
     * @return 词频向量，键为单词，值为出现次数
     */
    private Map<String, Integer> tf(String s) {
        Map<String, Integer> m = new HashMap<>();
        if (s == null) return m;
        String[] parts = s.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        for (String p : parts) {
            if (p.isEmpty()) continue;
            m.put(p, m.getOrDefault(p, 0) + 1);
        }
        return m;
    }
}

