package com.eugenezhu.voxforge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.stream.Stream;

/**
 * @projectName: VoxForge
 * @package: com.eugenezhu.voxforge.model
 * @className: CommandTemplate
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/11/17 上午11:39
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandTemplate {

    private String cmd;
    private String desc;
    private String os;
    private String shell;
}

