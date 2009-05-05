package gsn.beans.windowing;

import gsn.wrappers.AbstractWrapper;
import gsn.beans.DataField;
import gsn.beans.StreamElement;

public class MockWrapper extends AbstractWrapper {

        @Override
        public void finalize() {

        }

        @Override
        public DataField[] getOutputFormat() {
            return new DataField[]{};
        }

        @Override
        public String getWrapperName() {
            return "WrapperForTest1";
        }

        @Override
        public boolean initialize() {
            return true;
        }

        @Override
        public Boolean postStreamElement(StreamElement streamElement) {
            return super.postStreamElement(streamElement);
        }
}
