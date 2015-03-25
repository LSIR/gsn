package ch.epfl.gsn.metadata.core.model;

import ch.epfl.gsn.metadata.core.model.gsnjson.Field;
import ch.epfl.gsn.metadata.core.model.gsnjson.VirtualSensor;
import com.google.common.collect.Sets;
import org.springframework.data.geo.Point;

import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by kryvych on 11/03/15.
 */
@Named
public class GSNMetadataBuilder {

    public VirtualSensorMetadata buildVirtualSensorMetadata(VirtualSensor vsJson, String server) {
        Long startDatetime = vsJson.getProperties().getStats().getStartDatetime();
        Date fromDate = (startDatetime != null) ? new Date(startDatetime) : null;

        Long endDatetime = vsJson.getProperties().getStats().getEndDatetime();
        Date toDate = (endDatetime != null) ? new Date(endDatetime) : null;

        Double x = vsJson.getGeometry().getCoordinates().get(0);
        Double y = vsJson.getGeometry().getCoordinates().get(1);

        Point location = null;
        if (x != null && y != null) {
            location = new Point(x, y);
        }
        boolean isPublic = !Boolean.parseBoolean(vsJson.getProperties().getAccessProtected());


        VirtualSensorMetadata virtualSensorMetadata = new VirtualSensorMetadata(vsJson.getProperties().getVsName(),
                server, fromDate, toDate, location, isPublic);

        List<Field> fields = vsJson.getProperties().getFields();
        for (Field field : fields) {
            virtualSensorMetadata.addObservedProperty(new ObservedProperty(null, field.getUnit(), field.getName(), field.getType()));
        }

        virtualSensorMetadata.setDescription(vsJson.getProperties().getDescription());
        return virtualSensorMetadata;


    }

    public GridMetadata buildGridMetadata(VirtualSensor vsJson, String server) {
        Long startDatetime = vsJson.getProperties().getStats().getStartDatetime();
        Date fromDate = (startDatetime != null) ? new Date(startDatetime) : null;

        Long endDatetime = vsJson.getProperties().getStats().getEndDatetime();
        Date toDate = (endDatetime != null) ? new Date(endDatetime) : null;

        Double x = vsJson.getGeometry().getCoordinates().get(0);
        Double y = vsJson.getGeometry().getCoordinates().get(1);

        Point location = null;
        if (x != null && y != null) {
            location = new Point(x, y);
        }

        boolean isPublic = !Boolean.parseBoolean(vsJson.getProperties().getAccessProtected());


        GridMetadata gridMetadata = new GridMetadata(vsJson.getProperties().getVsName(),
                server, fromDate, toDate, location, isPublic);

        List<Field> fields = vsJson.getProperties().getFields();
        int i = 0;
        List<List<String>> values = vsJson.getProperties().getValues();

        //ToDo: Implement building grid!!!
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase("ncols")) {
            }
        }

        return gridMetadata;


    }

    public boolean isGrid(VirtualSensor sensor) {
        List<Field> fields = sensor.getProperties().getFields();
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase("grid")) {
                return true;
            }
        }
        return false;
    }


}
