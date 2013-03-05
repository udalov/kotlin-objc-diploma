#pragma once

#define failWithMsg(...) fprintf(stderr, __VA_ARGS__), exit(1)
#define failAssert(msg) failWithMsg("Assertion failed: %s (%s:%d)\n", msg, __FILE__, __LINE__)
#define assertWithMessage(condition, message) do { if (!(condition)) failAssert(message); } while (0)
#define assertNotNull(o) assertWithMessage(o, "'" #o "' cannot be null")
#define assertTrue(cond) assertWithMessage(cond, "'" #cond "' should be true")
#define assertFalse(cond) assertWithMessage(!cond, "'" #cond "' should be false")
#define assertEquals(o1, o2) assertWithMessage((o1) == (o2), "'" #o1 "' is not equal to '" #o2 "'")
