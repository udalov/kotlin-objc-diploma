@class A;

typedef void (*F01)();

typedef int (*F02)();

typedef void * (*F03)();

typedef int * (*F04)();

typedef void (*F05)(void);

typedef void (*F06)(void *);

typedef void (*F07)(int);

typedef void (*F08)(int *);

typedef void (*F09)(int, int *, void *, int);

typedef void (*F10)(void (*)());

typedef void (*F11)(void (*)(void));

typedef void (*F12)(int, ...);

typedef void (*F13)(void (*)(void), ...);

typedef void (*F14)(void (*)(int, ...));

typedef A * (*F15)(id, id, SEL);

typedef void (*F16)(void (*)(int, const char *), void * (*)(), ...);


@interface A
- (F01) f01;
- (F02) f02;
- (F03) f03;
- (F04) f04;
- (F05) f05;
- (F06) f06;
- (F07) f07;
- (F08) f08;
- (F09) f09;
- (F10) f10;
- (F11) f11;
- (F12) f12;
- (F13) f13;
- (F14) f14;
- (F15) f15;
- (F16) f16;
@end
