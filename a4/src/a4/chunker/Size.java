package a4.chunker;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Size implements Externalizable
{
    public enum Unit
    {
        K(1024), M(1024 * 1024);

        private int byteCount;

        Unit(int byteCount)
        {
            this.byteCount = byteCount;
        }

        public int getByteCount()
        {
            return byteCount;
        }

        @Override
        public String toString()
        {
            switch(this)
            {
                case K:
                    return "K";
                case M:
                    return "M";
                default:
                    return "?";
            }
        }
    }

    private int unitCount;
    private Unit unit;

    public Size(int unitCount, Unit unit)
    {
        this.unitCount = unitCount;
        this.unit = unit;
    }

    // Meant only for reading over a stream
    public Size()
    {
        this.unitCount = 0;
        this.unit = null;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(unitCount);
        out.writeUTF(unit.toString());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.unitCount = in.readInt();
        switch(in.readUTF())
        {
            case "K":
                unit = Unit.K;
                break;
            case "M":
                unit = Unit.M;
                break;
            default:
                // throw an exception here
                break;
        }
    }

    public int getByteCount()
    {
        return unitCount * unit.getByteCount();
    }

    @Override
    public String toString()
    {
        return Integer.toString(unitCount) + unit.toString();
    }
}
