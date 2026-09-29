package sg.edu.iss.cats.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.repository.ApplicationRepository;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;
    
    // TODO: complete application validation
    @Transactional( // these were copied from choppee demo, feel free to change if needed
    propagation  = Propagation.REQUIRED,
    isolation    = Isolation.SERIALIZABLE,
    rollbackFor  = Exception.class,
    timeout      = 30
    )
    // public Application saveApplication(Application application) {
    public void saveApplication(Application application) { // for planning out purposes, 
        // TODO: add validation 
        // TODO: after validation, applicationRepository.save()

        if (application.getApplicationStatus() == ApplicationStatus.APPLIED){
            application.setApplicationStatus(ApplicationStatus.UPDATED);
        }
        application.setApplicationStatus(ApplicationStatus.APPLIED);
        applicationRepository.save(application);
       
    }


}
