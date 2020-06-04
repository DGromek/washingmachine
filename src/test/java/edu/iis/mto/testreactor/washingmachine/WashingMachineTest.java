package edu.iis.mto.testreactor.washingmachine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class WashingMachineTest {

    @Mock
    private DirtDetector dirtDetector;
    @Mock
    private Engine engine;
    @Mock
    private WaterPump waterPump;
    private WashingMachine washingMachine;
    private ProgramConfiguration unrelevantProgramConfiguration;
    private LaundryBatch properLaundryBatch;

    @BeforeEach
    void setUp() {
        washingMachine = new WashingMachine(dirtDetector, engine, waterPump);

        unrelevantProgramConfiguration = ProgramConfiguration.builder()
                                                             .withProgram(Program.SHORT)
                                                             .withSpin(false)
                                                             .build();

        properLaundryBatch = LaundryBatch.builder()
                                         .withMaterialType(Material.COTTON)
                                         .withWeightKg(4)
                                         .build();
    }

    @Test
    void overweightLaundryBatchShouldResultInFailureAndTooHeavyErrorCode() {
        LaundryBatch outweighedLaundryBatch = LaundryBatch.builder()
                                                          .withMaterialType(Material.COTTON)
                                                          .withWeightKg(21.37)
                                                          .build();

        LaundryStatus actual = washingMachine.start(outweighedLaundryBatch, unrelevantProgramConfiguration);
        LaundryStatus expected = LaundryStatus.builder()
                                              .withErrorCode(ErrorCode.TOO_HEAVY)
                                              .withResult(Result.FAILURE)
                                              .build();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void properLaundryBatchShouldResultInSuccess() {
        LaundryStatus actual = washingMachine.start(properLaundryBatch, unrelevantProgramConfiguration);

        LaundryStatus expected = LaundryStatus.builder()
                                              .withRunnedProgram(Program.SHORT)
                                              .withResult(Result.SUCCESS)
                                              .withErrorCode(ErrorCode.NO_ERROR)
                                              .build();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void waterPumpExceptionThrownShouldResultInFailure() throws WaterPumpException {
        doThrow(WaterPumpException.class).when(waterPump).pour(anyDouble());

        LaundryStatus actual = washingMachine.start(properLaundryBatch, unrelevantProgramConfiguration);
        LaundryStatus expected = LaundryStatus.builder()
                                              .withRunnedProgram(Program.SHORT)
                                              .withResult(Result.FAILURE)
                                              .withErrorCode(ErrorCode.WATER_PUMP_FAILURE)
                                              .build();

        Assertions.assertEquals(expected, actual);
    }
}
