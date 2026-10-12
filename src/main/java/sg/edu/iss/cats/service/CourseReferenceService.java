package sg.edu.iss.cats.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;

/** Admin-maintained lookups for the three canonical course types and training providers. */
@Service
public class CourseReferenceService {
    private final CourseCategoryRepository categories;
    private final TrainingProviderRepository providers;
    private final CourseRepository courses;

    public CourseReferenceService(CourseCategoryRepository categories,
            TrainingProviderRepository providers, CourseRepository courses) {
        this.categories = categories; this.providers = providers; this.courses = courses;
    }

    @Transactional(readOnly = true)
    public List<CourseCategory> categories() {
        return categories.findAll().stream().sorted((a,b) -> a.getCode().compareTo(b.getCode())).toList();
    }
    @Transactional(readOnly = true)
    public List<CourseCategory> enabledCategories() {
        return categories().stream().filter(CourseCategory::isEnabled).toList();
    }
    @Transactional(readOnly = true)
    public List<TrainingProvider> providers() { return providers.findAllByOrderByNameAsc(); }
    @Transactional(readOnly = true)
    public List<TrainingProvider> enabledProviders() { return providers.findByEnabledTrueOrderByNameAsc(); }

    @Transactional
    public void updateCategory(CourseType code, String name, boolean enabled) {
        CourseCategory category = categories.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown canonical course category"));
        if (name == null || name.isBlank() || name.strip().length() > 100)
            throw new IllegalArgumentException("Category label is required (max 100 characters)");
        category.setDisplayName(name.strip());
        category.setEnabled(enabled);
        categories.save(category);
    }

    @Transactional
    public void saveProvider(Integer id, String name, boolean enabled) {
        if (name == null || name.isBlank() || name.strip().length() > 150)
            throw new IllegalArgumentException("Provider name is required (max 150 characters)");
        String trimmed = name.strip();
        if (providers.existsByNameIgnoreCaseAndIdNot(trimmed, id == null ? -1 : id))
            throw new IllegalArgumentException("That training provider already exists");
        TrainingProvider provider = id == null ? new TrainingProvider() : providers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        provider.setName(trimmed);
        provider.setEnabled(enabled);
        providers.save(provider);
    }

    @Transactional
    public void deleteProvider(Integer id) {
        TrainingProvider provider = providers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        if (courses.existsByProvider_Id(id))
            throw new IllegalArgumentException("Provider has course history; deactivate it instead of deleting");
        providers.delete(provider);
    }

    /** Resolve untrusted form IDs to database records, and copy their authoritative display names. */
    @Transactional(readOnly = true)
    public void resolveCourse(Course course) {
        if (course == null || course.getCourseType() == null)
            throw new IllegalArgumentException("Course category is required");
        CourseCategory category = categories.findById(course.getCourseType())
                .orElseThrow(() -> new IllegalArgumentException("Course category is unavailable"));
        if (!category.isEnabled())
            throw new IllegalArgumentException("Course category is inactive");
        Integer id = course.getProviderId();
        if (id == null && course.getProvider() != null) id = course.getProvider().getId();
        if (id == null) throw new IllegalArgumentException("Select a training provider");
        TrainingProvider provider = providers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown training provider"));
        if (!provider.isEnabled()) throw new IllegalArgumentException("Training provider is inactive");
        course.setProvider(provider);
        course.setProviderId(provider.getId());
        course.setTrainingProvider(provider.getName());
    }
}
