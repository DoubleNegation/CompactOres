package doublenegation.mods.compactores;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import net.minecraft.state.Property;
import net.minecraft.util.IStringSerializable;

/** Slightly modified copy of net.minecraft.state.EnumProperty */
public class CompactOreProperty<T extends Comparable<T> & IStringSerializable> extends Property<T> {
    private final ImmutableSet<T> allowedValues;
    private final Map<String, T> nameToValue = Maps.newHashMap();

    public CompactOreProperty(String name, Class<T> valueClass, Collection<T> allowedValues) {
        super(name, valueClass);
        this.allowedValues = ImmutableSet.copyOf(allowedValues);

        for(T t : allowedValues) {
            String s = ((IStringSerializable)t).getString();
            if (this.nameToValue.containsKey(s)) {
                throw new IllegalArgumentException("Multiple values have the same name '" + s + "'");
            }

            this.nameToValue.put(s, t);
        }

    }

    public Collection<T> getAllowedValues() {
        return this.allowedValues;
    }

    public Optional<T> parseValue(String value) {
        return Optional.ofNullable(this.nameToValue.get(value));
    }

    public String getName(T value) {
        return ((IStringSerializable)value).getString();
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (p_equals_1_ instanceof CompactOreProperty && super.equals(p_equals_1_)) {
            CompactOreProperty<?> enumproperty = (CompactOreProperty)p_equals_1_;
            return this.allowedValues.equals(enumproperty.allowedValues) && this.nameToValue.equals(enumproperty.nameToValue);
        } else {
            return false;
        }
    }

    public int computeHashCode() {
        int i = super.computeHashCode();
        i = 31 * i + this.allowedValues.hashCode();
        i = 31 * i + this.nameToValue.hashCode();
        return i;
    }

}