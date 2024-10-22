package de.uniwue.informatik.praline.io.input.processdata;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class ProcessEdge {

    private Activity startActivity;
    private Activity endActivity;
    private float sumThroughputTime;
    private float avgThroughputTime;
    private int occurenceCount;
    public ProcessEdge(Activity startActivity, Activity endActivity) {
        this.startActivity = startActivity;
        this.endActivity = endActivity;
        this.sumThroughputTime = ChronoUnit.SECONDS.between(startActivity.getDate(), endActivity.getDate());
        this.avgThroughputTime = ChronoUnit.SECONDS.between(startActivity.getDate(), endActivity.getDate());
        this.occurenceCount = 0;
    }

    public void addOccurence(LocalDateTime startDate, LocalDateTime endDate) {
        occurenceCount += 1;
        sumThroughputTime += ChronoUnit.SECONDS.between(startDate, endDate);
        avgThroughputTime = sumThroughputTime / occurenceCount;
    }

    public Activity getStartActivity() {
        return startActivity;
    }
    public Activity getEndActivity() {
        return endActivity;
    }

    public float getAvgThroughputTime() {
        return avgThroughputTime;
    }

    public int getOccurenceCount() {
        return occurenceCount / 2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessEdge)) return false;
        ProcessEdge that = (ProcessEdge) o;
        return startActivity.equals(that.startActivity) && endActivity.equals(that.endActivity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startActivity, endActivity);
    }

    @Override
    public String toString() {
        return "Start: " + startActivity.getName() + " | End: " + endActivity.getName() +
                " | avg Throughput Time:" + avgThroughputTime + " | Occurence: " + occurenceCount;
    }
}
