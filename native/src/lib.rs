extern crate jni;
extern crate x86intrin;

use jni::JNIEnv;
use jni::objects::{JObject};
use jni::sys::{jbyteArray, jlongArray};

use x86intrin::{m256i, mm256_cmpeq_epi8, mm256_setr_epi8, mm256_movemask_epi8};

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_mison_IndexBuilder_00024_avx2Bitmaps_000241(env: JNIEnv, _: JObject, json_ref: JObject, bitmap_ref: JObject) -> () {
  let json = env.convert_byte_array(json_ref.into_inner() as jbyteArray).unwrap();
  let bitmap = bitmap_ref.into_inner() as jlongArray;
  let bitmap_len = env.get_array_length(bitmap).unwrap() / 5;

  env.set_long_array_region(bitmap, 0 * bitmap_len, &compute_bitmap(&json, bitmap_len as usize, '\\' as u8)).unwrap();
  env.set_long_array_region(bitmap, 1 * bitmap_len, &compute_bitmap(&json, bitmap_len as usize, '"' as u8)).unwrap();
  env.set_long_array_region(bitmap, 2 * bitmap_len, &compute_bitmap(&json, bitmap_len as usize, ':' as u8)).unwrap();
  env.set_long_array_region(bitmap, 3 * bitmap_len, &compute_bitmap(&json, bitmap_len as usize, '{' as u8)).unwrap();
  env.set_long_array_region(bitmap, 4 * bitmap_len, &compute_bitmap(&json, bitmap_len as usize, '}' as u8)).unwrap();
}

#[inline]
fn compute_bitmap(json_bytes: &[u8], bitmap_len: usize, mask_character: u8) -> Vec<i64> {
  let mask = fill_vector(mask_character as i8);
  let mut bitmap = Vec::with_capacity(bitmap_len);
  for i in 0..(bitmap_len - 1) {
    let lo = unsafe { mm256_movemask_epi8(mm256_cmpeq_epi8(to_vector(json_bytes, i * 64), mask)) };
    let hi = unsafe { mm256_movemask_epi8(mm256_cmpeq_epi8(to_vector(json_bytes, (i * 64) + 32), mask)) };
    bitmap.push(((u64::from(hi as u32) << 32) | u64::from(lo as u32)) as i64);
  }
  let lo = unsafe { mm256_movemask_epi8(mm256_cmpeq_epi8(to_vector(json_bytes, (bitmap_len - 1) * 64), mask)) };
  let hi = unsafe { mm256_movemask_epi8(mm256_cmpeq_epi8(to_partial_vector(json_bytes, ((bitmap_len - 1) * 64) + 32), mask)) };
  bitmap.push(((u64::from(hi as u32) << 32) | u64::from(lo as u32)) as i64);
  bitmap
}

#[inline(always)]
pub fn fill_vector(i: i8) -> m256i {
  mm256_setr_epi8(i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i)
}

#[inline(always)]
pub unsafe fn to_vector(s: &[u8], i: usize) -> m256i {
  mm256_setr_epi8(
    *s.as_ptr().offset(i as isize + 00) as i8,
    *s.as_ptr().offset(i as isize + 01) as i8,
    *s.as_ptr().offset(i as isize + 02) as i8,
    *s.as_ptr().offset(i as isize + 03) as i8,
    *s.as_ptr().offset(i as isize + 04) as i8,
    *s.as_ptr().offset(i as isize + 05) as i8,
    *s.as_ptr().offset(i as isize + 06) as i8,
    *s.as_ptr().offset(i as isize + 07) as i8,
    *s.as_ptr().offset(i as isize + 08) as i8,
    *s.as_ptr().offset(i as isize + 09) as i8,
    *s.as_ptr().offset(i as isize + 10) as i8,
    *s.as_ptr().offset(i as isize + 11) as i8,
    *s.as_ptr().offset(i as isize + 12) as i8,
    *s.as_ptr().offset(i as isize + 13) as i8,
    *s.as_ptr().offset(i as isize + 14) as i8,
    *s.as_ptr().offset(i as isize + 15) as i8,
    *s.as_ptr().offset(i as isize + 16) as i8,
    *s.as_ptr().offset(i as isize + 17) as i8,
    *s.as_ptr().offset(i as isize + 18) as i8,
    *s.as_ptr().offset(i as isize + 19) as i8,
    *s.as_ptr().offset(i as isize + 20) as i8,
    *s.as_ptr().offset(i as isize + 21) as i8,
    *s.as_ptr().offset(i as isize + 22) as i8,
    *s.as_ptr().offset(i as isize + 23) as i8,
    *s.as_ptr().offset(i as isize + 24) as i8,
    *s.as_ptr().offset(i as isize + 25) as i8,
    *s.as_ptr().offset(i as isize + 26) as i8,
    *s.as_ptr().offset(i as isize + 27) as i8,
    *s.as_ptr().offset(i as isize + 28) as i8,
    *s.as_ptr().offset(i as isize + 29) as i8,
    *s.as_ptr().offset(i as isize + 30) as i8,
    *s.as_ptr().offset(i as isize + 31) as i8,
  )
}

#[inline(always)]
pub unsafe fn to_partial_vector(s: &[u8], i: usize) -> m256i {
  let d = s.len() - i;
  mm256_setr_epi8(
    if d > 00 { *s.as_ptr().offset(i as isize + 00) as i8} else { 0 },
    if d > 01 { *s.as_ptr().offset(i as isize + 01) as i8} else { 0 },
    if d > 02 { *s.as_ptr().offset(i as isize + 02) as i8} else { 0 },
    if d > 03 { *s.as_ptr().offset(i as isize + 03) as i8} else { 0 },
    if d > 04 { *s.as_ptr().offset(i as isize + 04) as i8} else { 0 },
    if d > 05 { *s.as_ptr().offset(i as isize + 05) as i8} else { 0 },
    if d > 06 { *s.as_ptr().offset(i as isize + 06) as i8} else { 0 },
    if d > 07 { *s.as_ptr().offset(i as isize + 07) as i8} else { 0 },
    if d > 08 { *s.as_ptr().offset(i as isize + 08) as i8} else { 0 },
    if d > 09 { *s.as_ptr().offset(i as isize + 09) as i8} else { 0 },
    if d > 10 { *s.as_ptr().offset(i as isize + 10) as i8} else { 0 },
    if d > 11 { *s.as_ptr().offset(i as isize + 11) as i8} else { 0 },
    if d > 12 { *s.as_ptr().offset(i as isize + 12) as i8} else { 0 },
    if d > 13 { *s.as_ptr().offset(i as isize + 13) as i8} else { 0 },
    if d > 14 { *s.as_ptr().offset(i as isize + 14) as i8} else { 0 },
    if d > 15 { *s.as_ptr().offset(i as isize + 15) as i8} else { 0 },
    if d > 16 { *s.as_ptr().offset(i as isize + 16) as i8} else { 0 },
    if d > 17 { *s.as_ptr().offset(i as isize + 17) as i8} else { 0 },
    if d > 18 { *s.as_ptr().offset(i as isize + 18) as i8} else { 0 },
    if d > 19 { *s.as_ptr().offset(i as isize + 19) as i8} else { 0 },
    if d > 20 { *s.as_ptr().offset(i as isize + 20) as i8} else { 0 },
    if d > 21 { *s.as_ptr().offset(i as isize + 21) as i8} else { 0 },
    if d > 22 { *s.as_ptr().offset(i as isize + 22) as i8} else { 0 },
    if d > 23 { *s.as_ptr().offset(i as isize + 23) as i8} else { 0 },
    if d > 24 { *s.as_ptr().offset(i as isize + 24) as i8} else { 0 },
    if d > 25 { *s.as_ptr().offset(i as isize + 25) as i8} else { 0 },
    if d > 26 { *s.as_ptr().offset(i as isize + 26) as i8} else { 0 },
    if d > 27 { *s.as_ptr().offset(i as isize + 27) as i8} else { 0 },
    if d > 28 { *s.as_ptr().offset(i as isize + 28) as i8} else { 0 },
    if d > 29 { *s.as_ptr().offset(i as isize + 29) as i8} else { 0 },
    if d > 30 { *s.as_ptr().offset(i as isize + 30) as i8} else { 0 },
    if d > 31 { *s.as_ptr().offset(i as isize + 31) as i8} else { 0 }
  )
}