package de.uniwue.informatik.praline.io.input.processdata;

import java.time.LocalDateTime;
import java.util.*;

public class Case {

    private List<Activity> activities;
    private String caseId;

    public Case(String caseId) {
        this.caseId = caseId;
        this.activities = new ArrayList<>();
    }

    public void addActivity(Activity activity) {
        activities.add(activity);
        sortActivitiesInc();
    }

    public void sortActivitiesInc() {
        Collections.sort(activities, Comparator.comparing(Activity::getDate));
    }

    public void addStartAndEndActivity() {
        LocalDateTime firstActivityDate = activities.get(0).getDate();
        Activity startActivity = new Activity("Start", firstActivityDate.minusSeconds(1), "SYSTEM");
        addActivity(startActivity);
        LocalDateTime lastActivityDate = activities.get(activities.size() - 1).getDate();
        Activity endActivity = new Activity("Ende", lastActivityDate.plusSeconds(1), "SYSTEM");
        addActivity(endActivity);
    }

    public List<Activity> getActivities() {
        return activities;
    }

    public String getCaseId() {
        return caseId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Case)) return false;
        Case aCase = (Case) o;
        return Objects.equals(activities, aCase.activities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activities);
    }

    @Override
    public String toString() {
        String activitiesString = "";
        for (int i = 0; i < activities.size(); i++) {
            activitiesString += "\t" + activities.get(i) + "\n";
        }
        return "Case " + caseId + "\nActivities:\n" + activitiesString;
    }
}
