typedef int int_really;

@class A;

typedef A A_really;

@interface A
- (int_really) f_int;
- (int_really *) f_pint;
- (int_really **) f_ppint;

- (A_really *) f_a;
- (A_really **) f_pa;
@end

