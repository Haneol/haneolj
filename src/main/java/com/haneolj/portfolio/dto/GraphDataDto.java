package com.haneolj.portfolio.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GraphDataDto {
    private List<GraphNodeDto> nodes = new ArrayList<>();
    private List<GraphLinkDto> links = new ArrayList<>();
}
