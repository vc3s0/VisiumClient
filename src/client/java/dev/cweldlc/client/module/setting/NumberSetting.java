package dev.cweldlc.client.module.setting;

public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    @Override
    public void setValue(Double value) {
        double clamped = Math.max(min, Math.min(max, value));
        double rounded = Math.round(clamped / step) * step;
        super.setValue(rounded);
    }

    public float getSliderProgress() {
        return (float) ((getValue() - min) / (max - min));
    }

    public void setFromProgress(float progress) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        setValue(min + progress * (max - min));
    }
}
