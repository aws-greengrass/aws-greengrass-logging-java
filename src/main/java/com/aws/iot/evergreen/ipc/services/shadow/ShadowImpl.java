package com.aws.iot.evergreen.ipc.services.shadow;

import com.aws.iot.evergreen.ipc.services.shadow.models.GetThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.GetThingShadowResult;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ShadowImpl implements  Shadow {
    @Override
    public GetThingShadowResult getThingShadow(GetThingShadowRequest request) {
        String thingName = request.getThingName();

        GetThingShadowResult result = new GetThingShadowResult();
        throw new NotImplementedException();
    }

}
