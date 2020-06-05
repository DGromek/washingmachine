package edu.iis.mto.testreactor.washingmachine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WashingMachineTest {

    @Mock
    private DirtDetector dirtDetector;
    @Mock
    private Engine engine;
    @Mock
    private WaterPump waterPump;
    @Mock
    private WashingMachine washingMachine;
    private ProgramConfiguration unrelevantProgramConfiguration;
    private LaundryBatch properLaundryBatch;
    private Program defaultProgram = Program.SHORT;

    @BeforeEach
    void setUp() {
        washingMachine = new WashingMachine(dirtDetector, engine, waterPump);

        unrelevantProgramConfiguration = ProgramConfiguration.builder()
                                                             .withProgram(defaultProgram)
                                                             .withSpin(true)
                                                             .build();

        properLaundryBatch = LaundryBatch.builder()
                                         .withMaterialType(Material.COTTON)
                                         .withWeightKg(4)
                                         .build();
    }

    //State tests
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
                                              .withRunnedProgram(defaultProgram)
                                              .withResult(Result.SUCCESS)
                                              .withErrorCode(ErrorCode.NO_ERROR)
                                              .build();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void waterPumpExceptionThrownShouldResultInFailure() throws WaterPumpException {
        doThrow(WaterPumpException.class).when(waterPump)
                                         .pour(anyDouble());

        LaundryStatus actual = washingMachine.start(properLaundryBatch, unrelevantProgramConfiguration);
        LaundryStatus expected = LaundryStatus.builder()
                                              .withRunnedProgram(defaultProgram)
                                              .withResult(Result.FAILURE)
                                              .withErrorCode(ErrorCode.WATER_PUMP_FAILURE)
                                              .build();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void engineExceptionThrownShouldResultInFailure() throws EngineException {
        doThrow(EngineException.class).when(engine)
                                      .runWashing(anyInt());

        LaundryStatus actual = washingMachine.start(properLaundryBatch, unrelevantProgramConfiguration);
        LaundryStatus expected = LaundryStatus.builder()
                                              .withRunnedProgram(defaultProgram)
                                              .withResult(Result.FAILURE)
                                              .withErrorCode(ErrorCode.ENGINE_FAILURE)
                                              .build();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void properLaundryBatchWithAutodetectProgramAndDirtLevelAboveAverageShouldResultInSuccessWithLongProgram() {
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder()
                                                                        .withProgram(Program.AUTODETECT)
                                                                        .withSpin(true)
                                                                        .build();

        when(dirtDetector.detectDirtDegree(any())).thenReturn(new Percentage(100));
        LaundryStatus actual = washingMachine.start(properLaundryBatch, programConfiguration);

        LaundryStatus expected = LaundryStatus.builder()
                                              .withRunnedProgram(Program.LONG)
                                              .withResult(Result.SUCCESS)
                                              .withErrorCode(ErrorCode.NO_ERROR)
                                              .build();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void properLaundryBatchWithAutodetectProgramAndDirtLevelBelowAverageShouldResultInSuccessWithShortProgram() {
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder()
                                                                        .withProgram(Program.AUTODETECT)
                                                                        .withSpin(true)
                                                                        .build();

        when(dirtDetector.detectDirtDegree(any())).thenReturn(new Percentage(1));
        LaundryStatus actual = washingMachine.start(properLaundryBatch, programConfiguration);

        LaundryStatus expected = LaundryStatus.builder()
                                              .withRunnedProgram(Program.MEDIUM)
                                              .withResult(Result.SUCCESS)
                                              .withErrorCode(ErrorCode.NO_ERROR)
                                              .build();

        Assertions.assertEquals(expected, actual);
    }

    //Behaviour tests
    @Test
    void properLaundryBatchWithSpinShouldCallEngineAndWaterPump() throws WaterPumpException, EngineException {
        InOrder callingOrder = inOrder(waterPump, engine);

        washingMachine.start(properLaundryBatch, unrelevantProgramConfiguration);

        callingOrder.verify(waterPump)
                    .pour(properLaundryBatch.getWeightKg());
        callingOrder.verify(engine)
                    .runWashing(unrelevantProgramConfiguration.getProgram()
                                                              .getTimeInMinutes());
        callingOrder.verify(waterPump)
                    .release();
        callingOrder.verify(engine)
                    .spin();
    }

    @Test
    void properLaundryBatchWithAutodetectProgramShouldCallDirtDetector() {
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder()
                                                                        .withProgram(Program.AUTODETECT)
                                                                        .withSpin(true)
                                                                        .build();

        washingMachine.start(properLaundryBatch, programConfiguration);

        Mockito.verify(dirtDetector)
               .detectDirtDegree(properLaundryBatch);

    }
}
