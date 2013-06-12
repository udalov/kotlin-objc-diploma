#import "libtest.h"

@implementation IncreaseCounter
- (void) increase {
    counter++;
}

- (int) get {
    return counter;
}
@end





@implementation SumOfManyPrimitives
+ (double) calculate: (int) i withShort: (short) s withLong: (long) j withFloat: (float) f withDouble: (double) d {
    return i + s + j + f + d;
}
@end






Singleton *instance = 0;

@implementation Singleton
+ (void) createInstance {
    instance = [[Singleton alloc] init];
}

+ (Singleton *) getInstance {
    return instance;
}
@end





@implementation Mirror
+ (id) get: (id) object {
    return object;
}
@end





@implementation InvokeCallback
+ (void) invoke: (void (*)()) callback {
    callback();
}
@end
