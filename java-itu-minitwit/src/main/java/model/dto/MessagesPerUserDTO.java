package model.dto;

import spark.Request;
import utilities.IRequests;

public class MessagesPerUserDTO extends DTO {
    public String username;
    public Integer userId;
    public Object flash;

    public static MessagesPerUserDTO fromRequest(Request request, IRequests requests) {
        var dto = new MessagesPerUserDTO();
        dto.latest = request.queryParams("latest");
        dto.authorization = request.headers("Authorization");
        dto.username = requests.getParam(":username", request).get();

        return dto;

    }

}
