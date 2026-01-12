/*
 * Copyright (C) 2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT.
 */
package tn.eluea.kgpt.features.textactions.domain;

import java.util.UUID;

public class CustomTextAction {
    public String id;
    public String name;
    public String prompt;
    public boolean enabled;

    public CustomTextAction() {
        // Default constructor for JSON
    }

    public CustomTextAction(String name, String prompt, boolean enabled) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.prompt = prompt;
        this.enabled = enabled;
    }

    public CustomTextAction(String id, String name, String prompt, boolean enabled) {
        this.id = id;
        this.name = name;
        this.prompt = prompt;
        this.enabled = enabled;
    }
}
