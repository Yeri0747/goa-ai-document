package es.upm.api.resources.dtos;

import es.upm.api.data.entities.LineItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LineItemResponseDto {
    private String name;
    private String quantity;
    private String price;
    private String unitPrice;

    public LineItemResponseDto(LineItem lineItem) {
        if (lineItem != null) {
            BeanUtils.copyProperties(lineItem, this);
        }
    }
}
