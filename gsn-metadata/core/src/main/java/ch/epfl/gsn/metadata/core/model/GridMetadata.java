package ch.epfl.gsn.metadata.core.model;

import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by kryvych on 11/03/15.
 */
@Document(collection = "grid")
public class GridMetadata extends GSNMetadata {
    private int ncolumn;
    private int nrows;
    private int xllcorner;
    private int yllcorner;
    private int cellsize;

    public GridMetadata(String name, String server, Date toDate, Date fromDate, Point location, boolean isPublic) {
        super(name, server, toDate, fromDate, location, isPublic);
    }

    public int getNcolumn() {
        return ncolumn;
    }

    public void setNcolumn(int ncolumn) {
        this.ncolumn = ncolumn;
    }

    public int getNrows() {
        return nrows;
    }

    public void setNrows(int nrows) {
        this.nrows = nrows;
    }

    public int getXllcorner() {
        return xllcorner;
    }

    public void setXllcorner(int xllcorner) {
        this.xllcorner = xllcorner;
    }

    public int getYllcorner() {
        return yllcorner;
    }

    public void setYllcorner(int yllcorner) {
        this.yllcorner = yllcorner;
    }

    public int getCellsize() {
        return cellsize;
    }

    public void setCellsize(int cellsize) {
        this.cellsize = cellsize;
    }

    @Override
    public boolean isGrid() {
        return true;
    }
}
