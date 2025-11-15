package com.eugenezhu.voxforge.model;

import lombok.Data;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: AsrResponse
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/21 下午7:14
 */
@Data
public class AsrResponse {
    private String reqid;
    private String operation;
    private DataInfo data; 

    @Data
    public static class DataInfo {
        private AudioInfo audio_info;  
        private ResultInfo result;   

        @Data
        public static class AudioInfo {
            private Integer duration;
        }

        @Data
        public static class ResultInfo {
            private AdditionsInfo additions;
            private String text;

            @Data
            public static class AdditionsInfo {
                private String duration;
            }
        }
    }
}

