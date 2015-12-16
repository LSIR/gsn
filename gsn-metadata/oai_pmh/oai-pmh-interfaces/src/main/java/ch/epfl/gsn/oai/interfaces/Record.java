package ch.epfl.gsn.oai.interfaces;

import java.util.Date;

/**
 * Created by kryvych on 07/09/15.
 */
public interface Record {

    String getOAIIdentifier();

    Date getDateStamp();

    boolean isDeleted();
}
