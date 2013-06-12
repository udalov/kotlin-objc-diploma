#import <Foundation/NSObject.h>

@interface IncreaseCounter : NSObject {
    int counter;
}

- (void) increase;

- (int) get;
@end




@interface SumOfManyPrimitives : NSObject
+ (double) calculate: (int) i withShort: (short) s withLong: (long) s withFloat: (float) f withDouble: (double) d;
@end




@interface Singleton : NSObject
+ (void) createInstance;

+ (Singleton *) getInstance;
@end




@interface Mirror : NSObject
+ (id) get: (id) object;
@end





@interface InvokeCallback : NSObject
+ (void) invoke: (void (*)()) callback;
@end
