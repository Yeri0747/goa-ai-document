package es.upm.api.resources.dtos;

import es.upm.api.data.entities.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDto {
    private String id;
    private String name;
    private Long sizeInfo;
    private String url;
    private LocalDateTime uploadDate;

    public DocumentDto(Document document) {
        BeanUtils.copyProperties(document, this);
    }
}
