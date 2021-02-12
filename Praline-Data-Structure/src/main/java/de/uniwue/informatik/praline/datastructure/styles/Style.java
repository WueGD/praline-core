package de.uniwue.informatik.praline.datastructure.styles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Style {

    /*==========
     * Default values
     *==========*/

    public static final String DEFAULT_DESCRIPTION = "no description";


    /*==========
     * Instance variables
     *==========*/

    private String description;


    /*==========
     * Constructors
     *==========*/

    protected Style() {
        this(null);
    }

    @JsonCreator
    protected Style(
            @JsonProperty("description") final String description
    ) {
        this.description = description == null ? DEFAULT_DESCRIPTION : description;
    }


    /*==========
     * Getters & Setters
     *==========*/

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
