package dev.rehm.lingua.llm;

import java.util.UUID;

public class LlmProfile {


    private UUID uuid;
    private String model;
    private Object think;
    private Integer numCtx;
    private Integer numPredict;
    private Double temperature;
    private Double topP;
    private Double minP;
    private Integer topK;
    private Double repeatPenalty;

    public LlmProfile(Double repeatPenalty, Integer topK, Double minP,
                      Double topP, Double temperature, Integer numPredict,
                      Integer numCtx, Object think, String model, UUID uuid) {
        this.repeatPenalty = repeatPenalty;
        this.topK = topK;
        this.minP = minP;
        this.topP = topP;
        this.temperature = temperature;
        this.numPredict = numPredict;
        this.numCtx = numCtx;
        this.think = think;
        this.model = model;
        this.uuid = uuid;
    }

    public LlmProfile(String model, Object think, Integer numCtx,
                      Integer numPredict, Double temperature, Double topP,
                      Double minP, Integer topK, Double repeatPenalty) {
        this.model = model;
        this.think = think;
        this.numCtx = numCtx;
        this.numPredict = numPredict;
        this.temperature = temperature;
        this.topP = topP;
        this.minP = minP;
        this.topK = topK;
        this.repeatPenalty = repeatPenalty;
    }

    public LlmProfile() {
    }

    public UUID getUuid() {
        return uuid;
    }


    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Object isThink() {
        return think;
    }

    public void setThink(Object think) {
        this.think = think;
    }

    public Integer getNumCtx() {
        return numCtx;
    }

    public void setNumCtx(Integer numCtx) {
        this.numCtx = numCtx;
    }

    public Integer getNumPredict() {
        return numPredict;
    }

    public void setNumPredict(Integer numPredict) {
        this.numPredict = numPredict;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Double getMinP() {
        return minP;
    }

    public void setMinP(Double minP) {
        this.minP = minP;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Double getRepeatPenalty() {
        return repeatPenalty;
    }

    public void setRepeatPenalty(Double repeatPenalty) {
        this.repeatPenalty = repeatPenalty;
    }
}
