typedef int int_really;

@class A;

typedef A A_really;

@interface A
- (int_really) i;
- (int_really *) pi;
- (int_really **) ppi;

- (A_really *) la;
- (A_really **) pla;
@end

