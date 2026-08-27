package kuke.board.common.snowflake;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Snowflake 단위 테스트")
class SnowflakeTest {
	Snowflake snowflake = new Snowflake();

	@Test
	@DisplayName("여러 스레드에서 동시에 발급해도 모든 ID는 유일하고, 각 스레드 내에서는 오름차순이다")
	void nextIdTest() throws ExecutionException, InterruptedException {
		// given
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		List<Future<List<Long>>> futures = new ArrayList<>();
		int repeatCount = 1000;
		int idCount = 1000;

		// when
		for (int i = 0; i < repeatCount; i++) {
			futures.add(executorService.submit(() -> generateIdList(snowflake, idCount)));
		}

		// then
		List<Long> result = new ArrayList<>();
		for (Future<List<Long>> future : futures) {
			List<Long> idList = future.get();
			for (int i = 1; i < idList.size(); i++) {
				assertThat(idList.get(i)).isGreaterThan(idList.get(i - 1));
			}
			result.addAll(idList);
		}
		assertThat(result.stream().distinct().count()).isEqualTo(repeatCount * idCount);

		executorService.shutdown();
	}

	@Test
	@DisplayName("시스템 시계가 뒤로 가면(lastTimeMillis보다 작아지면) IllegalStateException을 던진다")
	void nextIdThrowsWhenClockMovesBackward() throws NoSuchFieldException, IllegalAccessException {
		// given: push lastTimeMillis far into the future to simulate the
		// system clock having moved backward, without touching the real clock
		Field lastTimeMillis = Snowflake.class.getDeclaredField("lastTimeMillis");
		lastTimeMillis.setAccessible(true);
		lastTimeMillis.setLong(snowflake, System.currentTimeMillis() + 1_000_000L);

		assertThatThrownBy(snowflake::nextId)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Invalid Time");
	}

	List<Long> generateIdList(Snowflake snowflake, int count) {
		List<Long> idList = new ArrayList<>();
		while (count-- > 0) {
			idList.add(snowflake.nextId());
		}
		return idList;
	}

	@Test
	@DisplayName("스레드 10개로 100만 개 ID를 발급하는 데 걸리는 시간을 출력한다 (성능 참고용)")
	void nextIdPerformanceTest() throws InterruptedException {
		// given
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		int repeatCount = 1000;
		int idCount = 1000;
		CountDownLatch latch = new CountDownLatch(repeatCount);

		// when
		long start = System.nanoTime();
		for (int i = 0; i < repeatCount; i++) {
			executorService.submit(() -> {
				generateIdList(snowflake, idCount);
				latch.countDown();
			});
		}

		latch.await();

		long end = System.nanoTime();
		System.out.println("times = %s ms".formatted((end - start) / 1_000_000));

		executorService.shutdown();
	}
}