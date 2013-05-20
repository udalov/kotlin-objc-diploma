@interface NSObject
@end

@class B;

@interface A : NSObject
- (A *) a;
- (B *) b;
- (A *) aWithB: (B *) b;
@end

@interface B : NSObject
@end
