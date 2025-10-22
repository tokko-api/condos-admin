package com.condos.shared.dto;

import java.util.List;

public record AuthRes(String token, String orgId, List<String> roles, PublicUser user) {}