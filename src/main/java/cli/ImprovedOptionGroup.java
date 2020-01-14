package cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;

import java.util.Iterator;

public class ImprovedOptionGroup extends OptionGroup {
    private String groupName = null;

    public ImprovedOptionGroup() {

    }

    public ImprovedOptionGroup(final String groupName) {
        this.groupName = groupName;

    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        Iterator<Option> iter = this.getOptions().iterator();
        buff.append("[");

        while (iter.hasNext()) {
            Option option = (Option) iter.next();
            if (option.getOpt() != null) {
                buff.append("-");
                buff.append(option.getOpt());
            } else {
                buff.append("--");
                buff.append(option.getLongOpt());
            }

            if (option.getDescription() != null) {
                buff.append(" ");
                buff.append(option.getDescription());
            }

            if (iter.hasNext()) {
                buff.append(", ");
            }
        }

        buff.append("]");
        return buff.toString();
    }
}
