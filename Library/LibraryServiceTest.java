package Library;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers.*;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {
    @Mock
    private EmailProvider emailProvider;
    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private LibraryService libraryService;

    // TC1: test for null resource id - return false
    @Test
    void nullResourceIdTest() throws DatabaseFailureException, EmailFailureException{
        boolean result = libraryService.checkoutResource(null, "example@uca.edu");

        assertFalse(result);
        verifyNoInteractions(resourceRepository);
        verifyNoInteractions(emailProvider);
    }

    // TC2: test for unavailable resource - should return false
    @Test
    void  unavailableResourceTest() throws DatabaseFailureException, EmailFailureException{
        UUID resourceId = UUID.randomUUID();
        when(resourceRepository.isResourceAvailable(resourceId)).thenReturn(false);

        boolean result = libraryService.checkoutResource(resourceId, "example@uca.edu");

        assertFalse(result);
        verify(resourceRepository).isResourceAvailable(resourceId);
        verify(resourceRepository, never()).updateStatus(any(), anyBoolean());
        verifyNoInteractions(emailProvider);
    }

    // TC3: test for update failure - should throw DatabaseFailureException
    @Test
    void updateFailureTest() throws DatabaseFailureException, EmailFailureException{
        UUID resourceId = UUID.randomUUID();
        when(resourceRepository.isResourceAvailable(resourceId)).thenReturn(true);
        when(resourceRepository.updateStatus(resourceId, false)).thenReturn(false);

        DatabaseFailureException ex = assertThrows(
                DatabaseFailureException.class,
                () -> libraryService.checkoutResource(resourceId, "example@uca.edu")
        );

        assertEquals("Database Failed!" + "Could not check out item.", ex.getMessage());
        verify(emailProvider, never()).sendEmail(anyString(), anyString());
    }

    // TC4: test for no email sent - should throw EmailFailureException
    @Test
    void emailFailureTest() throws DatabaseFailureException, EmailFailureException{
        UUID resourceId = UUID.randomUUID();
        when(resourceRepository.isResourceAvailable(resourceId)).thenReturn(true);
        when(resourceRepository.updateStatus(resourceId, false)).thenReturn(true);
        when(emailProvider.sendEmail(eq("example@uca.edu"), anyString())).thenReturn(false);

        EmailFailureException ex = assertThrows(
                EmailFailureException.class,
                () -> libraryService.checkoutResource(resourceId, "example@uca.edu")
        );

        assertEquals("Email Failed!"+"Could not send email.", ex.getMessage());
    }

    // TC5: test for successful email sent - should return true
    @Test
    void emailSentTest() throws DatabaseFailureException, EmailFailureException{
        UUID resourceId = UUID.randomUUID();
        when(resourceRepository.isResourceAvailable(resourceId)).thenReturn(true);
        when(resourceRepository.updateStatus(resourceId,false)).thenReturn(true);
        when(emailProvider.sendEmail(eq("example@uca.edu"), anyString())).thenReturn(true);

        boolean result = libraryService.checkoutResource(resourceId, "example@uca.edu");

        assertTrue(result);
        verify(resourceRepository).isResourceAvailable(resourceId);
        verify(resourceRepository).updateStatus(resourceId, false);
        verify(emailProvider).sendEmail("Resource ID: " + eq("example@uca.edu")
                + " checked out.", contains(resourceId.toString()));
    }

}

