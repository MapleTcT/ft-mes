package com.supcon.supfusion.file.server.service.bo;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoBO {

    private String fileName;

    private String filePath;

    private String fileType;

    private Long fileSize;

    private Long lastModified;

}
