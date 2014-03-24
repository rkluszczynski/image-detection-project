package pl.info.rkluszczynski.image.web.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
@Qualifier("templateImageResources")
public class TemplateImageResources {
    private static Logger logger = LoggerFactory.getLogger(TemplateImageResources.class);

    private Map<String, Resource> imageResourcesMap;
    private List<TemplateResourceItem> imageResourcesItems;

    @Autowired
    public TemplateImageResources(ApplicationContext applicationContext) {
        imageResourcesMap = Maps.newHashMap();
        imageResourcesItems = Lists.newArrayList();
        try {
            Resource[] resources = applicationContext.getResources("classpath:image-data/patterns/*");
            for (Resource resource : resources) {
                Resource templateResource = resource;
                String templateResourceFilename = resource.getFilename();
                String templateResourceName = resource.getFilename();

                logger.info("RESOURCE: {}", resource.getURL().toString());
                imageResourcesMap.put(templateResourceFilename, templateResource);
                imageResourcesItems.add(new TemplateResourceItem(templateResourceFilename, templateResourceName));
            }
        } catch (IOException e) {
            logger.warn("Problem with loading image patterns resources!", e);
        }
    }

    public BufferedImage getTemplateImage(String resourceKeyValue) {
        try {
            InputStream inputStream = imageResourcesMap.get(resourceKeyValue).getInputStream();
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.close();
            return bufferedImage;
        } catch (IOException e) {
            logger.warn("Problem reading template image from resource!", e);
        }
        return null;
    }

    public List<TemplateResourceItem> getTemplateItems() {
        return imageResourcesItems;
    }

    public boolean isResourceKeyValueNotValid(String resourceKeyValue) {
        return !imageResourcesMap.containsKey(resourceKeyValue);
    }
}
