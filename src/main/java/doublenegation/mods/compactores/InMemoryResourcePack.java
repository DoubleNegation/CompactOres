package doublenegation.mods.compactores;

import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InMemoryResourcePack implements IResourcePack {

    private String name;
    private Map<String, Supplier<byte[]>> data;
    private Predicate<String> doesActuallyExist;

    public InMemoryResourcePack(String name, Map<String, Supplier<byte[]>> data, Predicate<String> doesActuallyExist) {
        this.name = name;
        this.data = data;
        this.doesActuallyExist = doesActuallyExist;
    }

    private InputStream s(Supplier<byte[]> data) {
        return new ByteArrayInputStream(data.get());
    }

    @Override
    public InputStream getRootResourceStream(String fileName) {
        if(fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Must be a root filename");
        }
        if(data.containsKey(fileName)) {
            return s(data.get(fileName));
        }
        return null;
    }

    @Override
    public InputStream getResourceStream(ResourcePackType type, ResourceLocation location) throws IOException {
        String path = type.getDirectoryName() + "/" + location.getNamespace() + "/" + location.getPath();
        if(data.containsKey(path)) return s(data.get(path));
        else throw new FileNotFoundException(type + ", " + location);
    }

    @Override
    public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType type, String namespaceIn, String pathIn, int maxDepthIn, Predicate<String> filterIn) {
        return data.keySet().stream().filter(s -> s.contains("/")).filter(doesActuallyExist).map(s -> {
                    String[] tk = s.split("/");
                    String typeStr = tk[0];
                    String namespace = tk[1];
                    String path = s.substring(typeStr.length() + namespace.length() + 2);
                    String filename = tk[tk.length - 1];
                    return new String[]{typeStr, namespace, path, filename};
                }).filter(tk -> tk[0].equals(type.getDirectoryName()))
                .filter(tk -> tk[1].startsWith(namespaceIn))
                .filter(tk -> tk[2].startsWith(pathIn))
                .filter(tk -> filterIn.test(tk[3]))
                .filter(tk -> tk[2].split("/").length - 1 <= maxDepthIn)
                .map(tk -> new ResourceLocation(tk[1], tk[2]))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean resourceExists(ResourcePackType type, ResourceLocation location) {
        String path = type.getDirectoryName() + "/" + location.getNamespace() + "/" + location.getPath();
        return data.containsKey(path) && doesActuallyExist.test(path);
    }

    @Override
    public Set<String> getResourceNamespaces(ResourcePackType type) {
        return data.keySet().stream().filter(s -> s.startsWith(type.getDirectoryName())).map(s -> s.split("/")[1])
                .collect(Collectors.toSet());
    }

    @Nullable
    @Override
    public <T> T getMetadata(IMetadataSectionSerializer<T> deserializer) throws IOException {
        // copied from net.minecraft.resources.VanillaPack
        try (InputStream inputstream = this.getRootResourceStream("pack.mcmeta")) {
            return ResourcePack.getResourceMetadata(deserializer, inputstream);
        } catch (FileNotFoundException | RuntimeException var16) {
            return null;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isHidden() {
        return true;
    }
}
